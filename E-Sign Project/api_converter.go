package util

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
)

type Request struct {
	ImgParameter       string  `json:"imgParameter"`
	PdfBase64          string  `json:"pdfBase64"`
	Username           string  `json:"username"`
	Url                string  `json:"url"`
	Disclaimer         string  `json:"disclaimer"`
	DisclaimerFontSize float64 `json:"disclaimerFontSize"`
}

func GenerateFinalDocument(ctx *gin.Context, pdfSource string, imgParameter string, urlQr string, targetPath string, sourceApk string, usernamePengaju string, disclaimerBsre string, disclaimerFontSize float64) error {

	url := os.Getenv("API_URL_CONVERTER") + "/generate-document-final?timestamp=123"
	fmt.Println("URL:>", url)

	var username string
	var err error
	var valid bool
	if sourceApk != "internal" {
		//if API, get from request
		username = usernamePengaju
	} else {
		//if internal get from token (context)
		username, _, err = ExtractUsernameListPeranByContext(ctx)
		if err != nil {
			fmt.Println("token expired")
			return errors.New("token expired")
		}
	}

	fmt.Println("username |" + username + "|")

	valid, err = VerifyDocument(ctx, pdfSource, imgParameter,
		urlQr, targetPath, sourceApk, usernamePengaju)

	if err != nil {
		return err
	} else if !valid {
		fmt.Println("error parameter image")
		return errors.New("error parameter image")
	}
	// fmt.Println("imgparameter", strings.TrimSpace(imgParameter[0:1000]))
	// fmt.Println("length", len(imgParameter))

	var jsonStr = Request{ImgParameter: imgParameter, PdfBase64: pdfSource, Username: username, Url: urlQr, Disclaimer: disclaimerBsre, DisclaimerFontSize: disclaimerFontSize}

	fmt.Println("pdfSource", strings.TrimSpace(pdfSource[0:30]))
	fmt.Println("length", len(pdfSource))
	payloadMarshalled, err := json.Marshal(jsonStr)
	if err != nil {
		fmt.Println("error marshall json")
		return errors.New("error marshall json")
	}
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(payloadMarshalled))

	if err != nil {
		fmt.Println("error setup API")
		return errors.New("error setup API")
	}

	req.Header.Set("X-Custom-Header", "myvalue")
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{Timeout: 3 * time.Minute}
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println(err)
		fmt.Println("error API " + resp.Status + " " + err.Error())
		return errors.New("error API " + resp.Status + " " + err.Error())
	}
	defer resp.Body.Close()

	if !strings.Contains(resp.Status, "200") {
		fmt.Println(err)
		fmt.Println("error API " + resp.Status)
		return errors.New("error API " + resp.Status)
	}

	fmt.Println("response Status:", resp.Status)
	fmt.Println("response Headers:", resp.Header)
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		fmt.Println("error read body")
		return errors.New("error read body")
	}

	fmt.Println("saving stamped file to " + targetPath)
	err = os.WriteFile(targetPath, body, 0644)
	if err != nil {
		return err
	}
	// fmt.Println("response Body:", string(body))

	return nil
}

func VerifyDocument(ctx *gin.Context, pdfSource string, imgParameter string, urlQr string, targetPath string, sourceApk string, usernamePengaju string) (bool, error) {
	url := os.Getenv("API_URL_CONVERTER") + "/verify-document?timestamp=123"
	fmt.Println("URL:>", url)

	var username string
	var err error
	if sourceApk != "internal" {
		//if API, get from request
		username = usernamePengaju
	} else {
		//if internal get from token (context)
		username, _, err = ExtractUsernameListPeranByContext(ctx)
		if err != nil {
			fmt.Println("token expired")
			return false, errors.New("token expired")
		}
	}

	fmt.Println("username |" + username + "|")
	// fmt.Println("imgparameter", strings.TrimSpace(imgParameter[0:1000]))
	// fmt.Println("length", len(imgParameter))

	var jsonStr = Request{ImgParameter: imgParameter, PdfBase64: pdfSource, Username: username, Url: urlQr}

	fmt.Println("pdfSource", strings.TrimSpace(pdfSource[0:30]))
	fmt.Println("length", len(pdfSource))
	payloadMarshalled, err := json.Marshal(jsonStr)
	if err != nil {
		fmt.Println("error marshall json")
		return false, errors.New("error marshall json")
	}
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(payloadMarshalled))

	if err != nil {
		fmt.Println("error setup API")
		return false, errors.New("error setup API")
	}

	req.Header.Set("X-Custom-Header", "myvalue")
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{Timeout: 3 * time.Minute}
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println(err)
		fmt.Println("error API " + resp.Status + " " + err.Error())
		return false, errors.New("error API " + resp.Status + " " + err.Error())
	}
	defer resp.Body.Close()

	if !strings.Contains(resp.Status, "200") {
		fmt.Println(err)
		fmt.Println("error API " + resp.Status)
		return false, errors.New("error API " + resp.Status)
	}

	fmt.Println("response Status:", resp.Status)
	fmt.Println("response Headers:", resp.Header)
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		fmt.Println("error read body")
		return false, errors.New("error read body")
	}
	status := string(body)
	if status == "true" {
		return true, nil
	} else {
		return false, nil
	}

}
