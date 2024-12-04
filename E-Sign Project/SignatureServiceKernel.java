package com.example.digitalSign.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.example.digitalSign.model.ImgParameter;
import com.example.digitalSign.model.ImgParameterNew;
import com.example.digitalSign.utils.TimeMillis;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.fasterxml.jackson.databind.JsonSerializable.Base;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;

import java.util.Base64;
import java.util.Iterator;
import java.util.List;
 
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.text.DocumentException;

@Service
public class SignatureServiceKernel {
    @Value("${esign.folder.surat}")
    private String folderSurat;

    @Value("${watermark}")
    private String watermark;

    // Preview dari dokumen sblm disimpan
    public String previewStamp(String pdfBase64, String imgParameter, String username, String url, String disclaimer, Float disclaimerFontSize, Boolean isFinal) throws IOException, DocumentException, WriterException {
        System.out.println(pdfBase64.substring(0, 30));
        String destination =  folderSurat + "/" + username;
        Files.createDirectories(Paths.get(destination));

        ObjectMapper objectMapper = new ObjectMapper();
        List<ImgParameterNew> imgParameters = objectMapper.readValue(imgParameter, new TypeReference<List<ImgParameterNew>>(){});
        Long timestamp = TimeMillis.uniqueCurrentTimeMS();
        String filename = "sign-tmp-" + timestamp.toString() + ".pdf";
        String path = destination + "/" + filename;

        String filename1 = "sign-tmp-" + timestamp.toString() + "-1.pdf";
        String path1 = destination + "/" + filename1;

        File file = new File(path);

        try ( FileOutputStream fos = new FileOutputStream(file); ) {
            // To be short I use a corrupted PDF string, so make sure to use a valid one if you want to preview the PDF file
            //String b64 = "JVBERi0xLjUKJYCBgoMKMSAwIG9iago8PC9GaWx0ZXIvRmxhdGVEZWNvZGUvRmlyc3QgMTQxL04gMjAvTGVuZ3==";
            byte[] decoder = Base64.getDecoder().decode(pdfBase64.replace("data:application/pdf;base64,", ""));
            fos.write(decoder);
            //System.out.println("PDF File Saved1" + pdfBase64);
          } catch (Exception e) {
            e.printStackTrace();
          }

        Iterator<ImgParameterNew> imgParameterIterator = imgParameters.iterator();

        PdfReader pdfreader = new PdfReader(path);
        PdfWriter pdfwriter = new PdfWriter(path1);
 
        // Creating a PdfDocument object.
        // passing PdfWriter object constructor of
        // pdfDocument.
        PdfDocument pdfdocument
            = new PdfDocument(pdfreader, pdfwriter);
 
        // Creating a Document and passing pdfDocument
        // object
        Document document = new Document(pdfdocument);
        while (imgParameterIterator.hasNext()) {

            ImgParameterNew imgParameter1 = imgParameterIterator.next();

            String imageFile = imgParameter1.getBase64Src();
            if(imgParameter1.getId().equals("QRCode") && isFinal) {

                BitMatrix matrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE,
                    imgParameter1.getWidth().intValue(), imgParameter1.getHeight().intValue());
                MatrixToImageConfig config = new MatrixToImageConfig(0xFF000000, 0x00FFFFFF);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(matrix, "png", bos, config);
                imageFile = Base64.getEncoder().encodeToString(bos.toByteArray()); // base64 encode
            }

            //System.out.println(imageFile + "usher");
            ImageData data = null;
            if(imageFile.contains("data:image/jpg;base64,")) {
                imageFile = imageFile.replaceAll("data:image/jpg;base64,", "");
                data = ImageDataFactory.createJpeg(Base64.getDecoder().decode(imageFile));
            } else {
                imageFile = imageFile.replaceAll("data:image/png;base64,", "");
                data = ImageDataFactory.createPng(Base64.getDecoder().decode(imageFile));

            }
            
            //data.setHeight(imgParameter1.getHeight());
            //data.setInterpolation(true);
            //data
            //data.setWidth(imgParameter1.getWidth());
            if(imgParameter1.getPage() > 0) {
                Image image = new Image(data);
                Float originalWidth = document.getPdfDocument().getPage(imgParameter1.getPage()).getMediaBox().getWidth();

                Float scale = originalWidth / imgParameter1.getPdfWidth() ;
                //Float heightScale = originalHeight / imgParameter1.getPdfHeight(); 

                // image.setFixedPosition(imgParameter1.getPage(), 100, 100);
                //image.setPageNumber(Integer.parseInt(imgParameter1.getPage()));
                Float y = scale * (imgParameter1.getPdfHeight() - imgParameter1.getPdfY()
                    - imgParameter1.getHeight());
                Float x = scale * imgParameter1.getPdfX();
                image.setHeight(scale * imgParameter1.getHeight());
                image.setWidth(scale * imgParameter1.getWidth());

                System.out.println("image width and height: " + imgParameter1.getHeight() + " " + imgParameter1.getWidth());
                System.out.println("image x and y: " + imgParameter1.getPdfX() + " " + y);
                System.out.println("Page dimension width and height: " + document.getPdfDocument().getPage(imgParameter1.getPage()).getMediaBox().getWidth()
                        + " " + document.getPdfDocument().getPage(imgParameter1.getPage()).getMediaBox().getHeight());



                image.setFixedPosition(imgParameter1.getPage(), x.floatValue(), y.floatValue());
                document.add(image);
            }
        }
        
        //DISCLAIMER 
        // System.out.println("disclaimer: "+ disclaimer);
        if (disclaimer != null) {
            for(int ii = 1; ii <= pdfdocument.getNumberOfPages(); ii++) {
                String[] texts = disclaimer.split("\\|");
                PdfCanvas pdfCanvas = new PdfCanvas(pdfdocument.getPage(ii));
                PdfFont font = PdfFontFactory.createFont();
                // float fontSize = pdfdocument.getPage(ii).getPageSize().getWidth()  * 1 / 60;
                System.out.println("fontSize: "+ disclaimerFontSize);
                pdfCanvas.setFontAndSize(font, disclaimerFontSize);
                pdfCanvas.beginText();
                
                for(int d = 0; d < texts.length; d++) {
                    float textWidth = font.getWidth(texts[d], disclaimerFontSize);
                    double x = (pdfdocument.getPage(ii).getPageSize().getWidth() - textWidth) / 2;
                    // double y = pdfdocument.getPage(ii).getPageSize().getHeight()  * fontSize * texts.length * 2 / 1000;
                    double y = 1.5 * disclaimerFontSize * texts.length;
                    if (texts.length > 1) {
                        if (d == 0) {
                            pdfCanvas.moveText(x, y);
                        } else {
                            float textWidthPrev = font.getWidth(texts[d-1], disclaimerFontSize);
                            double xPrev = (pdfdocument.getPage(ii).getPageSize().getWidth() - textWidthPrev) / 2;
                            y = -disclaimerFontSize;
                            pdfCanvas.moveText(x - xPrev, y);
                        }    
                    } else {
                        pdfCanvas.moveText(x, y);
                    }
                    System.out.println("text: "+ texts[d] + ", x: " + x+", y: " + y);
                    pdfCanvas.showText(texts[d]);
                }
                pdfCanvas.endText();
            }
        }
        document.close();
        file.delete();
        

        return path1;
    }


    public boolean verifyDocument(String pdfBase64, String imgParameter, String username, String url, Boolean isFinal) 
    throws IOException, DocumentException, WriterException {
            System.out.println(pdfBase64.substring(0, 30));
            String destination =  folderSurat + "/" + username;
            Files.createDirectories(Paths.get(destination));
    
            ObjectMapper objectMapper = new ObjectMapper();
            List<ImgParameterNew> imgParameters = objectMapper.readValue(imgParameter, new TypeReference<List<ImgParameterNew>>(){});
            Long timestamp = TimeMillis.uniqueCurrentTimeMS();
            String filename = "sign-tmp-" + timestamp.toString() + ".pdf";
            String path = destination + "/" + filename;
    
    
            File file = new File(path);
    
            try ( FileOutputStream fos = new FileOutputStream(file); ) {
                // To be short I use a corrupted PDF string, so make sure to use a valid one if you want to preview the PDF file
                //String b64 = "JVBERi0xLjUKJYCBgoMKMSAwIG9iago8PC9GaWx0ZXIvRmxhdGVEZWNvZGUvRmlyc3QgMTQxL04gMjAvTGVuZ3==";
                byte[] decoder = Base64.getDecoder().decode(pdfBase64.replace("data:application/pdf;base64,", ""));
                fos.write(decoder);
                //System.out.println("PDF File Saved1" + pdfBase64);
              } catch (Exception e) {
                e.printStackTrace();
              }
    
            Iterator<ImgParameterNew> imgParameterIterator = imgParameters.iterator();
    
            PdfReader pdfreader = new PdfReader(path);
     
            // Creating a PdfDocument object.
            // passing PdfWriter object constructor of
            // pdfDocument.
            PdfDocument pdfdocument
                = new PdfDocument(pdfreader);
     
            // Creating a Document and passing pdfDocument
            // object
            Document document = new Document(pdfdocument);
            int pageNumber = document.getPdfDocument().getNumberOfPages();

            while (imgParameterIterator.hasNext()) {
                ImgParameterNew imgParameter1 = imgParameterIterator.next();
                if(imgParameter1.getPage() > pageNumber) {
                    document.close();
                    return false;
                }
            }
            document.close();
            return true;
    }

    // Preview dari dokumen sblm disimpan
    public String watermarkDocument(String pdfBase64, String username, String disclaimer, Float disclaimerFontSize) throws IOException, DocumentException {
        Long timestamp = TimeMillis.uniqueCurrentTimeMS();
        String destination =  folderSurat + "/" + username;
        Files.createDirectories(Paths.get(destination));

        String filename = "sign-tmp-" + timestamp.toString() + ".pdf";
        String path = destination + "/" + filename;

        String filename1 = "sign-tmp-" + timestamp.toString() + "-1.pdf";
        String path1 = destination + "/" + filename1;

        File file = new File(path);

        try ( FileOutputStream fos = new FileOutputStream(file); ) {
            // To be short I use a corrupted PDF string, so make sure to use a valid one if you want to preview the PDF file
            //String b64 = "JVBERi0xLjUKJYCBgoMKMSAwIG9iago8PC9GaWx0ZXIvRmxhdGVEZWNvZGUvRmlyc3QgMTQxL04gMjAvTGVuZ3==";
            byte[] decoder = Base64.getDecoder().decode(pdfBase64.replace("data:application/pdf;base64,", "")
                .replace("data:application/octet-stream;base64,", ""));
            fos.write(decoder);
            //System.out.println("PDF File Saved1" + pdfBase64);
          } catch (Exception e) {
            e.printStackTrace();
          }


        PdfReader pdfreader = new PdfReader(path);
        PdfWriter pdfwriter = new PdfWriter(path1);
        
        // Creating a PdfDocument object.
        // passing PdfWriter object constructor of
        // pdfDocument.
        PdfDocument pdfdocument
            = new PdfDocument(pdfreader, pdfwriter);
 
        // Creating a Document and passing pdfDocument
        // object
        Document document = new Document(pdfdocument);
        for(int ii = 1; ii <= pdfdocument.getNumberOfPages(); ii++) {
            watermark = watermark.replaceAll("data:image/png;base64,", "");
            ImageData data = ImageDataFactory.createPng(Base64.getDecoder().decode(watermark));
            Image image = new Image(data);
            // image.setHeight(imgParameter1.getHeight());
            // image.setWidth(imgParameter1.getWidth());
            float width = pdfdocument.getPage(ii).getPageSize().getWidth() / 8;
            float height = pdfdocument.getPage(ii).getPageSize().getHeight() / 4 ;
            image.setFixedPosition(ii, width, height);
            document.add(image);

            //DISCLAIMER 
            if (disclaimer != null) {
                // for(int ii = 1; ii <= pdfdocument.getNumberOfPages(); ii++) {
                String[] texts = disclaimer.split("\\|");
                PdfCanvas pdfCanvas = new PdfCanvas(pdfdocument.getPage(ii));
                PdfFont font = PdfFontFactory.createFont();
                // float fontSize = pdfdocument.getPage(ii).getPageSize().getWidth()  * 1 / 60;
                System.out.println("fontSize: "+ disclaimerFontSize);
                pdfCanvas.setFontAndSize(font, disclaimerFontSize);
                pdfCanvas.beginText();
                
                for(int d = 0; d < texts.length; d++) {
                    float textWidth = font.getWidth(texts[d], disclaimerFontSize);
                    double x = (pdfdocument.getPage(ii).getPageSize().getWidth() - textWidth) / 2;
                    // double y = pdfdocument.getPage(ii).getPageSize().getHeight()  * fontSize * texts.length * 2 / 1000;
                    double y = 1.5 * disclaimerFontSize * texts.length;
                    if (texts.length > 1) {
                        if (d == 0) {
                            pdfCanvas.moveText(x, y);
                        } else {
                            float textWidthPrev = font.getWidth(texts[d-1], disclaimerFontSize);
                            double xPrev = (pdfdocument.getPage(ii).getPageSize().getWidth() - textWidthPrev) / 2;
                            y = -disclaimerFontSize;
                            pdfCanvas.moveText(x - xPrev, y);
                        }    
                    } else {
                        pdfCanvas.moveText(x, y);
                    }
                    System.out.println("text: "+ texts[d] + ", x: " + x+", y: " + y);
                    pdfCanvas.showText(texts[d]);
                }
                pdfCanvas.endText();
                // }
            }
        }
            
        document.close();
        file.delete();
        

        return path1;
    }

    // Preview dan Tanda Tangan untuk dokumen DRAGGABLE SIGNATURE dari ALUMNI
    public String previewStampOld(String pdfBase64, String imgParameter) throws IOException, DocumentException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ImgParameter> imgParameters = objectMapper.readValue(imgParameter, new TypeReference<List<ImgParameter>>(){});
        Long timestamp = TimeMillis.uniqueCurrentTimeMS();
        String filename = "sign-tmp-" + timestamp.toString() + ".pdf";
        String path = folderSurat + "/" + filename;

        String filename1 = "sign-tmp-" + timestamp.toString() + "-1.pdf";
        String path1 = folderSurat + "/" + filename1;

        File file = new File(path);

        try ( FileOutputStream fos = new FileOutputStream(file); ) {
            // To be short I use a corrupted PDF string, so make sure to use a valid one if you want to preview the PDF file
            //String b64 = "JVBERi0xLjUKJYCBgoMKMSAwIG9iago8PC9GaWx0ZXIvRmxhdGVEZWNvZGUvRmlyc3QgMTQxL04gMjAvTGVuZ3==";
            byte[] decoder = Base64.getDecoder().decode(pdfBase64);
            fos.write(decoder);
            System.out.println("PDF File Saved1" + pdfBase64);
          } catch (Exception e) {
            e.printStackTrace();
          }

        Iterator<ImgParameter> imgParameterIterator = imgParameters.iterator();

        PdfReader pdfreader = new PdfReader(path);
        PdfWriter pdfwriter = new PdfWriter(path1);
 
        // Creating a PdfDocument object.
        // passing PdfWriter object constructor of
        // pdfDocument.
        PdfDocument pdfdocument
            = new PdfDocument(pdfreader, pdfwriter);
 
        // Creating a Document and passing pdfDocument
        // object
        Document document = new Document(pdfdocument);
            while (imgParameterIterator.hasNext()) {

                ImgParameter imgParameter1 = imgParameterIterator.next();

                String imageFile = imgParameter1.getPageSrc(); 
                ImageData data = ImageDataFactory.create(imageFile); 
                data.setHeight(imgParameter1.getImgHeight().floatValue());
                data.setWidth(imgParameter1.getImgWidth().floatValue());
                Image image = new Image(data);
                image.setFixedPosition(Integer.parseInt(imgParameter1.getPage()), 100, 100);
                //image.setPageNumber(Integer.parseInt(imgParameter1.getPage()));
                Integer y = imgParameter1.getPdfHeight().intValue() - imgParameter1.getY().intValue()
                    - imgParameter1.getImgHeight().intValue();
                image.setFixedPosition(Integer.parseInt(imgParameter1.getPage()), imgParameter1.getX().floatValue(), y.floatValue());
                document.add(image);
            }
            document.close();
        

        return folderSurat + "/" + filename1;
    }
    
}
