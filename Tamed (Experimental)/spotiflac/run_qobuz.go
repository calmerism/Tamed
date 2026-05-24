package main

import (
	"fmt"
	"os"
	"encoding/json"
	"github.com/zarz/spotiflac_android/go_backend"
)

func main() {
	req := map[string]interface{}{
		"service": "qobuz",
		"track_name": "Merry Go Round",
		"artist_name": "BTS",
		"album_name": "Album",
		"output_dir": "./test_output",
		"filename_format": "{album_artist}/{album}/{tracknumber}. {title}",
		"quality": "LOSSLESS",
		"embed_metadata": true,
		"embed_lyrics": true,
		"embed_max_quality_cover": true,
	}

	reqBytes, _ := json.Marshal(req)
	
	resp, err := gobackend.DownloadTrack(string(reqBytes))
	if err != nil {
		fmt.Println("Error:", err)
		os.Exit(1)
	}
	
	fmt.Println("Response:", resp)
}
