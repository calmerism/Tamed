package main

import (
	"fmt"
	"regexp"
	"strings"
)

func tokens(s string) map[string]bool {
	re := regexp.MustCompile(`[^a-z0-9\s]`)
	s = re.ReplaceAllString(strings.ToLower(s), " ")
	words := strings.Fields(s)
	
	m := make(map[string]bool)
	for _, w := range words {
		if len(w) > 1 {
			m[w] = true
		}
	}
	return m
}

func overlapScore(a, b string) float64 {
	ta := tokens(a)
	tb := tokens(b)
	if len(ta) == 0 || len(tb) == 0 {
		return 0.0
	}
	intersection := 0
	for k := range ta {
		if tb[k] {
			intersection++
		}
	}
	union := len(ta) + len(tb) - intersection
	return float64(intersection) / float64(union)
}

func testScore(song, artist, itemSong, itemArtist string) {
	songScore := overlapScore(song, itemSong)
	artistScore := overlapScore(artist, itemArtist)
	finalScore := 0.0
	if songScore > 0.0 {
		finalScore = 0.65*songScore + 0.35*artistScore
	}
	fmt.Printf("songScore: %.2f, artistScore: %.2f, finalScore: %.2f\n", songScore, artistScore, finalScore)
}

func main() {
	testScore("Deadbeat", "Tame Impala", "Dracula", "Tame Impala")
	testScore("Deadbeat", "Tame Impala", "One More Year", "Tame Impala")
}
