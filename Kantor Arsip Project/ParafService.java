package com.pusilkom.arsipui.service;

import com.github.dandelion.datatables.core.ajax.DataSet;
import com.google.common.net.MediaType;
import com.pusilkom.arsipui.dto.form.search.DaftarApprovalSearchForm;
import com.pusilkom.arsipui.dto.table.DaftarApprovalItem;
import com.pusilkom.arsipui.dto.table.SuratDTO;
import com.pusilkom.arsipui.dto.view.PenggunaDetail;
import com.pusilkom.arsipui.model.User;
import com.pusilkom.arsipui.model.UserExample;
import com.pusilkom.arsipui.model.mapper.ParafMapper;
import com.pusilkom.arsipui.model.mapper.SuratMapper;
import com.pusilkom.arsipui.model.mapper.UserMapper;
import com.pusilkom.arsipui.util.DebugUtil;
import com.pusilkom.arsipui.util.FileUtil;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by ITF on 8/15/2019.
 */
@Service
@Transactional(readOnly = false)
public class ParafService {
    @Autowired
    ParafMapper parafMapper;
    @Autowired
    UserMapper userMapper;
    Logger log = LoggerFactory.getLogger(this.getClass());
    @Value("${folder.upload.surat}")
    private String folderUploadSurat;

    @Autowired
    UserService userService;

    @Autowired
    SuratService suratService;

    @Autowired
    DebugUtil d;

    @Autowired
    SuratMapper suratMapper;

    public List<User> getAllUser() {
        UserExample ex = new UserExample();
        return userMapper.selectByExample(ex);
    }

    public DataSet<DaftarApprovalItem> getDataSetApproval(DaftarApprovalSearchForm searchForm) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        List<DaftarApprovalItem> listItem = parafMapper.getDaftarApprovalItemBySearchForm(searchForm);
        //log.info(">>> daftar approval : {}", d.toString(listItem));
        for(DaftarApprovalItem item : listItem){
            if(item.getTanggalPembuatan() != null){
                SimpleDateFormat pattern = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                item.setTanggalPembuatanStr(pattern.format(item.getTanggalPembuatan()));

                Boolean hasilCek = cekWaktuDerajat(item.getDerajat(), item.getTanggalPembuatan());
                item.setFlagDerajat(hasilCek);

                if(item.getKepada()!= null && !item.getKepada().equals("")) {
                    if(item.getTipe() != null && item.getTipe()==2){
                        String listNama = suratService.convertListKepadaToString(item.getKepada());
                        item.setKepada(listNama);
                    }
                }
            }

        }
        Long totalItemFiltered = parafMapper.getTotalDaftarApprovalItemBySearchForm(searchForm);
        Long totalItem = totalItemFiltered;

        return new DataSet<DaftarApprovalItem>(listItem, totalItem, totalItemFiltered);
    }

    public DataSet<DaftarApprovalItem> getDataSetSign(DaftarApprovalSearchForm searchForm) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        List<DaftarApprovalItem> listItem = parafMapper.getDaftarSignItemBySearchForm(searchForm);
        //log.info(">>> daftar sign : {}", d.toString(listItem));
        for(DaftarApprovalItem item : listItem){
            if(item.getTanggalPembuatan() != null){
                SimpleDateFormat pattern = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                item.setTanggalPembuatanStr(pattern.format(item.getTanggalPembuatan()));

                Boolean hasilCek = cekWaktuDerajat(item.getDerajat(), item.getTanggalPembuatan());
                item.setFlagDerajat(hasilCek);
                if(item.getKepada()!= null && !item.getKepada().equals("")) {
                    if(item.getTipe() != null && item.getTipe()==2){
                        String listNama = suratService.convertListKepadaToString(item.getKepada());
                        item.setKepada(listNama);
                    }
                }
            }
        }
        Long totalItemFiltered = parafMapper.getTotalDaftarSignItemBySearchForm(searchForm);
        Long totalItem = totalItemFiltered;

        return new DataSet<DaftarApprovalItem>(listItem, totalItem, totalItemFiltered);
    }

    public DataSet<DaftarApprovalItem> getDataSetBsre(DaftarApprovalSearchForm searchForm) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        List<DaftarApprovalItem> listItem = parafMapper.getDaftarBsreItemBySearchForm(searchForm);
        //log.info(">>> daftar bsre : {}", d.toString(listItem));
        for(DaftarApprovalItem item : listItem){
            if(item.getTanggalPembuatan() != null){
                SimpleDateFormat pattern = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                item.setTanggalPembuatanStr(pattern.format(item.getTanggalPembuatan()));
            }
            if(item.getKepada()!= null && !item.getKepada().equals("")) {
                if(item.getTipe() != null && item.getTipe()==2){
                    String listNama = suratService.convertListKepadaToString(item.getKepada());
                    item.setKepada(listNama);
                }
            }
        }
        Long totalItemFiltered = parafMapper.getTotalDaftarBsreItemBySearchForm(searchForm);
        Long totalItem = totalItemFiltered;

        return new DataSet<DaftarApprovalItem>(listItem, totalItem, totalItemFiltered);
    }

    public void saveFile(MultipartFile file, SuratDTO suratDTO) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User detailUserLogin = userService.getUserDetailByUsername(authentication.getName());
            File fileFolder = null;
            fileFolder = createFileFolder ();
            TikaConfig config = TikaConfig.getDefaultConfig();
            MediaType mediaType = FileUtil.getMediaTypeBeforeDownload (file);
            MimeType mimeType = null;

            mimeType = config.getMimeRepository().forName(mediaType.toString());

            String extension = mimeType.getExtension();
            String saveFilename = "verified_"+suratDTO.getId();
            BufferedOutputStream outStream = null;

            File fileDestination = new File (fileFolder, saveFilename);
            outStream = new BufferedOutputStream(new FileOutputStream(fileDestination));

            FileCopyUtils.copy (file.getInputStream (), outStream);


            outStream.close ();
        } catch (MimeTypeException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File createFileFolder ()
    {
        File fileFolder = new File (
                folderUploadSurat + File.separator + "verified"
                        + File.separator);

        if (!fileFolder.exists ())
            fileFolder.mkdirs ();

        return fileFolder;
    }

    public Long getJumlahHarusApprove(){
        DaftarApprovalSearchForm searchForm = new DaftarApprovalSearchForm();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User detailUserLogin = userService.getUserDetailByUsername(authentication.getName());
        searchForm.setUserId(detailUserLogin.getId());
        searchForm.setStatusId(0);
        Long jmlApprove = parafMapper.getTotalDaftarApprovalItemBySearchForm(searchForm);
        return jmlApprove;
    }

    public Long getJumlahHarusSign(){
        DaftarApprovalSearchForm searchForm = new DaftarApprovalSearchForm();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User detailUserLogin = userService.getUserDetailByUsername(authentication.getName());
        searchForm.setUserId(detailUserLogin.getId());
        searchForm.setStatusId(2);
        Long jmlApprove = parafMapper.getTotalDaftarSignItemBySearchForm(searchForm);
        return jmlApprove;
    }

    //check if tanggal pembuatan exceed tanggal derajat
    //false -> exceed, true -> elsewise
    public Boolean cekWaktuDerajat(String derajat, Date tanggalPembuatan){
        Integer jam = 0;
        if(derajat.equalsIgnoreCase("Segera"))  jam = 24;
        else if(derajat.equalsIgnoreCase("sangat segera"))  jam = 3;
        else if(derajat.equalsIgnoreCase("biasa"))  jam = 72;

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(tanggalPembuatan);
        cal2.add(Calendar.HOUR, jam);

        if(cal.compareTo(cal2) >= 0){
            return false;
        } else {
            return true;
        }
    }
}
