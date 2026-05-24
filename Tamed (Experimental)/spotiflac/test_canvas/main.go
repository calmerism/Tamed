package main

import (
	"fmt"
	gobackend "github.com/zarz/spotiflac_android/go_backend"
)

func main() {
	res, err := gobackend.FetchCanvasBySongArtist("the mountain", "gorillaz", "us")
	fmt.Printf("Error: %v\n", err)
	fmt.Printf("Result: %s\n", res)
}
