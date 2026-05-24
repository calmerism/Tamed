package main

import (
	"fmt"
	"io"
	"net/http"
	"regexp"
)

func main() {
	url := "https://music.apple.com/us/album/a/1837237742?i=1837237747"
	req, _ := http.NewRequest("GET", url, nil)
	req.Header.Set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		fmt.Println("Error:", err)
		return
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(resp.Body)
	
	re := regexp.MustCompile(`"video":"(https://mvod\.itunes\.apple\.com[^"]+\.m3u8)"`)
	matches := re.FindStringSubmatch(string(body))
	if len(matches) > 1 {
		fmt.Println("FOUND:", matches[1])
	} else {
		fmt.Println("NOT FOUND")
	}
}
