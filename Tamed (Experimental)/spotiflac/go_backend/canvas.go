/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package gobackend

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"regexp"
	"strings"
	"sync"
	"time"
)

// canvasArtworkResponse matches the CanvasArtwork data class in the Android canvas module.
type canvasArtworkResponse struct {
	Name     string `json:"name,omitempty"`
	Artist   string `json:"artist,omitempty"`
	AlbumID  string `json:"albumId,omitempty"`
	Static   string `json:"static,omitempty"`
	Animated string `json:"animated,omitempty"`
	VideoURL string `json:"videoUrl,omitempty"`
}

// canvasCacheEntry holds a cached result with an expiry timestamp.
type canvasCacheEntry struct {
	result    *canvasArtworkResponse
	expiresAt time.Time
}

var (
	canvasCache   sync.Map
	canvasCacheTTL = 30 * time.Minute
)

// FetchCanvasBySongArtist is the main exported function called by the Android app via gomobile.
func FetchCanvasBySongArtist(song, artist, storefront string) (string, error) {
	// Normalize inputs for searching
	song = normalizeCanvasSongTitle(song)
	artist = normalizeCanvasArtistName(artist)
	if strings.TrimSpace(song) == "" || strings.TrimSpace(artist) == "" {
		return "", fmt.Errorf("song and artist must not be empty")
	}
	if storefront == "" {
		storefront = "us"
	}

	cacheKey := strings.ToLower("v3|" + song + "|" + artist + "|" + storefront)
	if v, ok := canvasCache.Load(cacheKey); ok {
		entry := v.(canvasCacheEntry)
		if time.Now().Before(entry.expiresAt) {
			if entry.result == nil {
				return "", nil
			}
			b, err := json.Marshal(entry.result)
			if err != nil {
				return "", err
			}
			return string(b), nil
		}
		canvasCache.Delete(cacheKey)
	}

	result, err := fetchCanvasFromAppleMusic(song, artist, storefront)

	// Cache even a nil result to avoid hammering the API for missing songs.
	canvasCache.Store(cacheKey, canvasCacheEntry{
		result:    result,
		expiresAt: time.Now().Add(canvasCacheTTL),
	})

	if err != nil || result == nil {
		return "", err
	}

	b, marshalErr := json.Marshal(result)
	if marshalErr != nil {
		return "", marshalErr
	}
	return string(b), nil
}

// fetchCanvasFromAppleMusic performs the two-step lookup:
//  1. Search Apple Music for the song → get its Apple Music ID
//  2. Fetch the trickplay (motion artwork) URL for that ID
func fetchCanvasFromAppleMusic(song, artist, storefront string) (*canvasArtworkResponse, error) {
	client := NewAppleMusicClient()

	// Step 1: resolve the Apple Music song ID.
	searchResult, err := client.SearchSong(song, artist, 0)
	isAlbumSearch := false
	if err != nil || searchResult == nil || searchResult.ID == "" {
		// Fallback: search for the album itself if the song search failed.
		searchResult, err = client.SearchAlbum(song, artist)
		if err != nil || searchResult == nil || searchResult.ID == "" {
			return nil, fmt.Errorf("apple music search failed (tried song and album): %w", err)
		}
		isAlbumSearch = true
	}

	songID := searchResult.ID
	var animatedURL string

	// Step 2: fetch motion artwork (trickplay) from Apple Music.
	// Only try Paxsenix if it's a song search (paxsenix detail is song-specific)
	if !isAlbumSearch {
		animatedURL, _ = fetchAppleMusicTrickplay(songID, storefront)
	}

	if animatedURL == "" {
		// Fallback: Scrape the Apple Music page for the motion artwork.
		// We try the album page first if we have an AlbumID, as it's the most likely source for motion art.
		if searchResult.AlbumID != "" {
			animatedURL, _ = scrapeAppleMusicMotion(searchResult.AlbumID, "album", storefront)
		}
		// If that fails, try the song page itself.
		if animatedURL == "" && !isAlbumSearch {
			animatedURL, _ = scrapeAppleMusicMotion(songID, "song", storefront)
		}
	}

	if animatedURL == "" {
		return nil, fmt.Errorf("no animation found for id=%s", songID)
	}

	return &canvasArtworkResponse{
		Name:     song,
		Artist:   artist,
		AlbumID:  songID,
		Animated: animatedURL,
		VideoURL: animatedURL,
	}, nil
}

// appleMusicSongDetailResponse is a minimal parse of the Apple Music catalog response.
type appleMusicSongDetailResponse struct {
	Data []struct {
		Attributes struct {
			Name       string `json:"name"`
			ArtistName string `json:"artistName"`
			AlbumName  string `json:"albumName"`
			Artwork    struct {
				URL         string `json:"url"`
				HasP3Color  bool   `json:"hasP3"`
			} `json:"artwork"`
		} `json:"attributes"`
		Relationships struct {
			Albums struct {
				Data []struct {
					ID string `json:"id"`
				} `json:"data"`
			} `json:"albums"`
		} `json:"relationships"`
	} `json:"data"`
}

// paxSongDetailResponse is the structure returned by the paxsenix Apple Music proxy.
type paxSongDetailResponse struct {
	ID         string `json:"id"`
	SongName   string `json:"songName"`
	ArtistName string `json:"artistName"`
	AlbumName  string `json:"albumName"`
	// Trickplay / motion artwork fields that paxsenix exposes.
	TrickplayURL  string `json:"trickplayUrl"`
	AnimatedArtwork string `json:"animatedArtwork"`
	MotionArtwork   string `json:"motionArtwork"`
}

// fetchAppleMusicTrickplay fetches the motion artwork (trickplay) URL for a given Apple Music song ID.
// It uses the paxsenix proxy which exposes song-specific trickplay URLs.
// NOTE: We intentionally do NOT fall back to scraping the Apple Music web page HTML,
// because the "video" field in the HTML is the album-level animated artwork which is
// the same for every track on the album (e.g. Dracula's video showing for all Deadbeat songs).
func fetchAppleMusicTrickplay(songID, storefront string) (string, error) {
	httpClient := &http.Client{Timeout: 15 * time.Second}

	// --- Only source: paxsenix song detail endpoint (song-specific trickplay) ---
	detailURL := fmt.Sprintf("https://lyrics.paxsenix.org/apple-music/song?id=%s", songID)
	req, err := http.NewRequest("GET", detailURL, nil)
	if err != nil {
		return "", fmt.Errorf("failed to create paxsenix request: %w", err)
	}
	req.Header.Set("User-Agent", appUserAgent())
	req.Header.Set("Accept", "application/json")

	resp, err := httpClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("paxsenix request failed: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		return "", fmt.Errorf("paxsenix returned HTTP %d for song id=%s", resp.StatusCode, songID)
	}

	body, readErr := io.ReadAll(resp.Body)
	if readErr != nil {
		return "", fmt.Errorf("failed to read paxsenix response: %w", readErr)
	}

	var detail paxSongDetailResponse
	if jsonErr := json.Unmarshal(body, &detail); jsonErr != nil {
		return "", fmt.Errorf("failed to parse paxsenix response: %w", jsonErr)
	}

	if detail.TrickplayURL != "" {
		return detail.TrickplayURL, nil
	}
	if detail.AnimatedArtwork != "" {
		return detail.AnimatedArtwork, nil
	}
	if detail.MotionArtwork != "" {
		return detail.MotionArtwork, nil
	}

	return "", fmt.Errorf("no trickplay URL found for song id=%s", songID)
}

func scrapeAppleMusicMotion(id, itemType, storefront string) (string, error) {
	if storefront == "" {
		storefront = "us"
	}
	if itemType == "" {
		itemType = "song"
	}
	url := fmt.Sprintf("https://music.apple.com/%s/%s/%s", storefront, itemType, id)

	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return "", err
	}
	// Use a very modern desktop user agent to get the full JSON payload
	req.Header.Set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
	req.Header.Set("Accept", "text/html")

	client := NewMetadataHTTPClient(10 * time.Second)
	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		return "", fmt.Errorf("apple music page returned HTTP %d", resp.StatusCode)
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	bodyStr := string(body)

	if bodyStr != "" {
		// Pattern 1: Look for specific animation keys in JSON
		// We now support both .m3u8 (HLS) and .mp4 (fallback)
		keys := []string{
			"motionArtworkUrl", "editorialVideo", "animatedArtwork", "motionVideoUrl", 
			"motionArtwork", "videoUrl", "video", "hlsUrl", "trickplayUrl", "screenVideoUrl",
		}
		for _, key := range keys {
			reKey := regexp.MustCompile(fmt.Sprintf(`"%s"\s*:\s*"([^"]+\.(?:m3u8|mp4)[^"]*)"`, key))
			match := reKey.FindStringSubmatch(bodyStr)
			if len(match) > 1 {
				return strings.ReplaceAll(match[1], "\\/", "/"), nil
			}
		}

		// Pattern 2: Search for any mvod or mzstatic video URL that is an HLS playlist or MP4
		reVideo := regexp.MustCompile(`https://(?:mvod\.itunes|[^"]+mzstatic)\.apple\.com/[^"]+\.(?:m3u8|mp4)[^"]*`)
		videoMatch := reVideo.FindString(bodyStr)
		if videoMatch != "" {
			return strings.ReplaceAll(videoMatch, "\\/", "/"), nil
		}

		// Pattern 3: Broader search for any video URL that contains motion/trickplay/animated/editorial
		reAnyVideo := regexp.MustCompile(`https://[^\s"]+(?:motion|trickplay|animated|editorial|artwork|HLS)[^\s"]+\.(?:m3u8|mp4)[^\s"]*`)
		videoMatchAny := reAnyVideo.FindString(bodyStr)
		if videoMatchAny != "" {
			return strings.ReplaceAll(videoMatchAny, "\\/", "/"), nil
		}
	}

	// Fallback to US region if local region failed and we haven't tried US yet
	if storefront != "us" {
		return scrapeAppleMusicMotion(id, itemType, "us")
	}

	return "", fmt.Errorf("no motion artwork found in %s page source for id=%s", itemType, id)
}

func normalizeCanvasSongTitle(raw string) string {
	// 1. Remove [...]
	reBracket := regexp.MustCompile(`\s*\[[^]]*]`)
	stripped := reBracket.ReplaceAllString(raw, "")

	// 2. Remove (feat...) (ft...) (featuring...) (with...)
	reFeat := regexp.MustCompile(`(?i)\s*\((?:feat\.?|ft\.?|featuring|with)\b[^)]*\)`)
	stripped = reFeat.ReplaceAllString(stripped, "")

	// 3. Remove (Official Video), (MV), (Lyrics), (Deluxe Edition), etc.
	reVideo := regexp.MustCompile(`(?i)\s*\((?:official\s*)?(?:music\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix|deluxe|edition|expanded|anniversary|special|bonus|track)[^)]*\)`)
	stripped = reVideo.ReplaceAllString(stripped, "")

	// 4. Remove - Official Video, etc. at end
	reDashVideo := regexp.MustCompile(`(?i)\s*-\s*(?:official\s*)?(?:music\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix|deluxe|edition|expanded|anniversary|special|bonus|track)\b.*$`)
	stripped = reDashVideo.ReplaceAllString(stripped, "")

	// 5. Clean up whitespace
	reSpace := regexp.MustCompile(`\s+`)
	stripped = reSpace.ReplaceAllString(stripped, " ")
	stripped = strings.TrimSpace(stripped)

	// 6. Trim leading/trailing dashes and spaces
	stripped = strings.Trim(stripped, "- ")
	stripped = reSpace.ReplaceAllString(stripped, " ")

	return strings.TrimSpace(stripped)
}

func normalizeCanvasArtistName(raw string) string {
	// Extract the first artist if there are multiple (separated by comma, &, x, feat, etc.)
	reSplit := regexp.MustCompile(`(?i)(?:\s*,\s*|\s*&\s*|\s+×\s+|\s+x\s+|\bfeat\.?\b|\bft\.?\b|\bfeaturing\b|\bwith\b)`)
	parts := reSplit.Split(raw, 2)
	first := ""
	if len(parts) > 0 {
		first = parts[0]
	}

	reSpace := regexp.MustCompile(`\s+`)
	return strings.TrimSpace(reSpace.ReplaceAllString(first, " "))
}
