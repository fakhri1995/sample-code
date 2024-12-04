package com.pusilkom.arsipui.controller;

import com.github.dandelion.datatables.core.ajax.DataSet;
import com.github.dandelion.datatables.core.ajax.DatatablesCriterias;
import com.github.dandelion.datatables.core.ajax.DatatablesResponse;
import com.lowagie.text.DocumentException;
import com.pusilkom.arsipui.dto.form.cmd.BuatSuratCmd;
import com.pusilkom.arsipui.dto.form.search.DaftarRiwayatSearchForm;
import com.pusilkom.arsipui.dto.table.DaftarRiwayatItem;
import com.pusilkom.arsipui.dto.table.SuratSignDTO;
import com.pusilkom.arsipui.dto.view.KlasifikasiDetail;
import com.pusilkom.arsipui.dto.view.SifatDetail;
import com.pusilkom.arsipui.dto.view.UnitKerjaDetail;
import com.pusilkom.arsipui.model.Surat;
import com.pusilkom.arsipui.model.Template;
import com.pusilkom.arsipui.model.User;
import com.pusilkom.arsipui.model.mapper.KodeKlasifikasiMapper;
import com.pusilkom.arsipui.model.mapper.UserMapper;
import com.pusilkom.arsipui.service.SuratApproveService;
import com.pusilkom.arsipui.service.SuratService;
import com.pusilkom.arsipui.service.SuratSignService;
import com.pusilkom.arsipui.service.UserService;
import com.pusilkom.arsipui.util.DebugUtil;
import com.pusilkom.arsipui.util.RestValidatorUtil;
import com.pusilkom.arsipui.validator.form.cmd.BuatSuratCmdValidator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by ITF on 8/15/2019.
 */
@Controller
@Secured("ROLE_AUTHORIZED")
public class SuratController {
    @Autowired
    UserService userService;
    @Autowired
    UserMapper userMapper;
    @Autowired
    SuratService suratService;
    @Value("${folder.prefix}")
    private String folderPrefix;
    @Autowired
    DebugUtil d;
    Logger log = LoggerFactory.getLogger(this.getClass());
    @Autowired
    BuatSuratCmdValidator buatSuratCmdValidator;
    @Autowired
    RestValidatorUtil rv;
    @Autowired
    ThymeleafProperties properties;
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    SuratApproveService suratApproveService;
    @Autowired
    KodeKlasifikasiMapper kodeKlasifikasiMapper;
    @Autowired
    SuratSignService suratSignService;
    @Value("${folder.upload.surat}")
    private String folderUploadSurat;

    final static String ERROR_MSG = "Failed to process";

    @InitBinder("surat")
    protected void registerBuatSuratCmdValidator(WebDataBinder binder) {
        binder.setValidator(buatSuratCmdValidator);
    }

    @GetMapping(value = "/buat-surat")
    public String getSuratAdd(Model uiModel) {
        BuatSuratCmd surat = new BuatSuratCmd();
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        surat.setTanggalPembuatanStr(df.format(System.currentTimeMillis()));


        //log.info(">>> listSigner : {}", d.toString(listSigner));
        //log.info(">>> listApprover : {}", d.toString(listApprover));

        List<Template> listTemplate = suratService.getAllTemplate();

        String[] monthNames = {
                "Januari", "Februari", "Maret",
                "April", "Mei", "Juni", "Juli",
                "Agustus", "September", "Oktober",
                "November", "Desember" };
        Date currentDate = new Date(System.currentTimeMillis());
        uiModel.addAttribute("surat", surat);
        uiModel.addAttribute("cetak", false);
        uiModel.addAttribute("tanggal", currentDate.getDate()+" "+monthNames[currentDate.getMonth()]+" "+(1900+currentDate.getYear()));
        uiModel.addAttribute("listTemplate",listTemplate);
        uiModel.addAttribute("listKomentarApprover", null);
        uiModel.addAttribute("listKomentarSigner", null);
        uiModel.addAttribute("isSaveDraft", true);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());

        uiModel.addAttribute("headerName", suratService.headerName(new Surat()));
        uiModel.addAttribute("pembuat",detailUserLogin.getNama());
        return "buat_surat";
    }



    @RequestMapping(value = "/buat-surat", method = RequestMethod.POST)
    public String postSuratAdd(@Valid @ModelAttribute(value="surat") BuatSuratCmd buatSuratCmd, BindingResult result,
                               @RequestParam("fileSurat") MultipartFile fileSurat,
                               @RequestParam String action,
                               RedirectAttributes attributes, Model uiModel) throws IOException {
        log.info("start");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());

        uiModel.addAttribute("headerName", suratService.headerName(new Surat()));

        List<User> listApprover = userService.getAllUserByRole("approver");
        List<User> listSigner = userService.getAllUserByRole("signer");
        List<Template> listTemplate = suratService.getAllTemplate();
        //log.info(">>> formBuatSurat : {}", d.toString(buatSuratCmd));
        //log.info(">>> fileSurat : {}", d.toString(fileSurat));
        //log.info(">>> action : {}", d.toString(action));
        //log.info(">>> result : {}", d.toString(result.hasErrors()));

        boolean isNotPdf = false;
        boolean isExceedSize = false;
        boolean isSaveDraft = (action.equals("btnSaveDraft") ? true : false);

        Surat surat = null;
        if(buatSuratCmd.getId() != null) {
            surat = suratService.getSuratById(buatSuratCmd.getId());
        }

//        if(surat != null && surat.getStatusId() > -1) {
//            return "redirect:/buat-surat/" + surat.getId();
//        }


        // Don't save lampiran file if it is too big
        if(!fileSurat.isEmpty()){
            if(fileSurat.getSize()>27262976){
                isExceedSize = true;
            }
            isNotPdf = !fileSurat.getOriginalFilename().substring (fileSurat.getOriginalFilename().length()-3, fileSurat.getOriginalFilename().length ()).toUpperCase ().equals (new String("pdf").toUpperCase ());
        }

        Boolean pdfValid = !isNotPdf && !isExceedSize;
        Boolean valid = false;
        if(isSaveDraft) {
            valid = pdfValid;
        } else if(!isSaveDraft) {
            valid = pdfValid && !result.hasErrors();
        }

        if(pdfValid) {
            buatSuratCmd.setFile(fileSurat.getOriginalFilename().replace(" ", "_"));
        } else {
            fileSurat = null;
            buatSuratCmd.setFile(null);
        }

        if(!pdfValid || result.hasErrors()) {
            isSaveDraft = true;
        }

        Integer savedSuratId =  0;
        try {
            // case for koreksi
            if(!isSaveDraft && surat != null && surat.getId() != null && surat.getStatusId() > -1) {
                savedSuratId = suratService.saveCmdOnKoreksi(buatSuratCmd, fileSurat);
            } else {
                savedSuratId = suratService.saveCmd(buatSuratCmd, fileSurat, isSaveDraft);
            }
        } catch (Exception e) {
            attributes.addFlashAttribute("ERROR", "Gagal tambah surat");
            e.printStackTrace();
            return "redirect:/daftar-riwayat";
        }

        String errorText = "";
        if(isExceedSize) {
            errorText = errorText + " Ukuran File Lampiran Maksimal 25 MB";
        }

        if(isNotPdf) {
            errorText = errorText + " File Lampiran Harus Pdf";
        }



        // validation if there are errors
        if (!valid) {
            uiModel.addAttribute("idOrganisasi",buatSuratCmd.getIdOrganisasi());
            uiModel.addAttribute("idKodeKlasifikasi",buatSuratCmd.getIdKodeKlasifikasi());
            uiModel.addAttribute("listSigner",listSigner);
            uiModel.addAttribute("listApprover",listApprover);
            uiModel.addAttribute("listTemplate",listTemplate);
            uiModel.addAttribute("pembuat", detailUserLogin);
            uiModel.addAttribute("isSaveDraft", isSaveDraft);
            attributes.addAttribute("errorText", errorText);
            return "redirect:/buat-surat/"+savedSuratId;

        }

        // validation on success
        if(isSaveDraft) {
            attributes.addFlashAttribute("SUCCESS", "Berhasil tambah draft");
        } else {
            // case for koreksi
            if(!isSaveDraft && surat != null && surat.getId() != null && surat.getStatusId() > -1) {
                return "redirect:/detail-surat-paraf/" + surat.getId();
            }
            attributes.addFlashAttribute("SUCCESS", "Berhasil simpan surat");
        }


        return "redirect:/daftar-riwayat";

    }

    @GetMapping(value = "/download-template/{id}")
    public void downloadBerkas (@PathVariable("id") Integer id, HttpServletResponse response) {
        try {
            Template tem = suratService.selectTemplateByPrimaryKey(id);
            String filename = tem.getFile();
            String path = tem.getDir();

            response.setHeader ("Content-Disposition", "filename= \""
                    + filename + "\"");
            response.setContentType ("application/pdf");

            File file = new File (path);
            // download file
            //response.setContentType ("application/pdf,application/*");
            InputStream is = new FileInputStream(file);
            ServletOutputStream out = response.getOutputStream ();
            IOUtils.copy (is, out);
            out.flush ();
            out.close ();
            is.close ();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }

    @RequestMapping(value = "/daftar-riwayat", method = RequestMethod.POST)
    @ResponseBody
    public DatatablesResponse<DaftarRiwayatItem> postTableSearch(DaftarRiwayatSearchForm searchForm, HttpServletRequest httpServletRequest) {
        DatatablesCriterias criterias = DatatablesCriterias.getFromRequest(httpServletRequest);
        searchForm.setCriterias(criterias);

        DataSet<DaftarRiwayatItem> dataSet = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("cek-username"+ ((UserDetails) authentication.getPrincipal()).getUsername());
            User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());
            searchForm.setUserId(detailUserLogin.getId());
            //log.info(">>> searchForm : {}", d.toString(searchForm));
            dataSet = suratService.getDataSet(searchForm);
        } catch (Exception e) {
            log.error("TABLE SURAT : ", e);

        }

        return DatatablesResponse.build(dataSet, criterias);
    }

    @GetMapping(value = "/daftar-riwayat")
    public String indexGet(@Valid DaftarRiwayatSearchForm searchForm, BindingResult result,
                           Model uiModel) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("cek-username"+ ((UserDetails) authentication.getPrincipal()).getUsername());

        if (result.hasErrors()) {
            return "daftar_riwayat";
        }

        //suratService.migrateUser();
        uiModel.addAttribute("daftarRiwayatSearchForm", searchForm);

        return "daftar_riwayat";
    }

    @GetMapping(value = "/migrate-user")
    public String migrateUser(@Valid DaftarRiwayatSearchForm searchForm, BindingResult result,
                           Model uiModel) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("cek-username"+ ((UserDetails) authentication.getPrincipal()).getUsername());
        if (result.hasErrors()) {
            return "daftar_riwayat";
        }

        String migrationStatus;
        if(suratService.migrateUser()) {
            migrationStatus = "Migrasi Berhasil";
        } else {
            migrationStatus = "Ada kesalahan pada migrasi, mohon kontak IT Support";

        }
        uiModel.addAttribute("migrationStatus", migrationStatus);
        uiModel.addAttribute("daftarRiwayatSearchForm", searchForm);

        return "daftar_riwayat";
    }

    @GetMapping(value = "/detail-surat/{id}")
    public String getDetailSurat(Model uiModel, @PathVariable("id") Integer id) {
        Surat surat = suratService.getSuratById(id);
        uiModel.addAttribute("surat",surat);
        return "paraf/paraf-surat";
    }

    @GetMapping(value = "/home/fakhri/images/{path:.+}")
    public void downloadHeader (@PathVariable String path, HttpServletResponse response) {
        try {


            System.out.println("Path: "+path);
            response.setHeader ("Content-Disposition", "filename= \""
                    + path + "\"");
            response.setContentType ("image/png");

            File file = new File ("/home/fakhri/images/"+path);
            // download file
            //response.setContentType ("application/pdf,application/*");
            InputStream is = new FileInputStream(file);
            ServletOutputStream out = response.getOutputStream ();
            IOUtils.copy (is, out);
            out.flush ();
            out.close ();
            is.close ();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }

    @GetMapping(value = "/home/fakhri/cap/{path:.+}")
    public void downloadMakara(@PathVariable String path, HttpServletResponse response) {
        try {
            response.setHeader ("Content-Disposition", "filename= \""
                    + path + "\"");
            response.setContentType ("image/png");

            File file = new File ("/home/fakhri/cap/"+path);
            // download file
            //response.setContentType ("application/pdf,application/*");
            InputStream is = new FileInputStream(file);
            ServletOutputStream out = response.getOutputStream ();
            IOUtils.copy (is, out);
            out.flush ();
            out.close ();
            is.close ();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }

    @GetMapping(value = "/preview-no-surat/")
    public ResponseEntity previewNoSurat(@RequestParam(value = "idTipe") Integer idTipe, @RequestParam(value = "idSigner") Integer idSigner,
                                         @RequestParam(value = "idKodeKlasifikasi") Integer idKodeKlasifikasi, @RequestParam(value="idSurat") Integer idSurat) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        ResponseEntity responseEntity = null;
        String counter = "";
        if(idSurat!=-1){
            Surat suratDetail = suratService.getSuratById(idSurat);
            if(suratDetail.getNoSurat()!=null && !suratDetail.getNoSurat().isEmpty() && suratDetail.getTipe() == idTipe){
                String getCounter = suratService.getCounterNosurat(suratDetail.getNoSurat());
                if(!getCounter.equals("XX")) counter = getCounter;
            }
        }

        String previewNoSurat = suratService.previewNoSurat(idTipe, idSigner, idKodeKlasifikasi, false, counter);
        responseEntity = ResponseEntity.ok(previewNoSurat);
        return responseEntity;
    }

    @GetMapping(value = "/json-kode-klasifikasi/")
    public ResponseEntity getJSONKodeKlasifikasi(@RequestParam(value = "search", required = false) String kode) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        ResponseEntity responseEntity = null;
        //log.info(">>> kode : {}", d.toString(kode));
        Map<String, String> listKode = suratService.getJSONKodeKlasifikasi(kode);
        responseEntity = ResponseEntity.ok(listKode);
        //log.info(">>> listKode : {}", d.toString(listKode));
        return responseEntity;
    }

    @GetMapping(value = "/json-organisasi/")
    public ResponseEntity getJSONOrganisasi(@RequestParam(value = "search", required = false) String kode) {
        ResponseEntity responseEntity = null;
        //log.info(">>> kode : {}", d.toString(kode));
        Map<String, String> listKode = suratService.getJSONOrganisasi(kode);
        responseEntity = ResponseEntity.ok(listKode);
        //log.info(">>> listKode : {}", d.toString(listKode));
        return responseEntity;
    }

    @GetMapping(value = "/json-signer/")
    public ResponseEntity getJSONSigner(@RequestParam(value = "search", required = false) String kode) {
        ResponseEntity responseEntity = null;
        //log.info(">>> signer : {}", d.toString(kode));
        Map<String, String> listKode = suratService.getJSONUserByRole(kode, "signer");

        responseEntity = ResponseEntity.ok(listKode);
        //log.info(">>> listKode : {}", d.toString(listKode));
        return responseEntity;
    }

    @GetMapping(value = "/json-approver/")
    public ResponseEntity getJSONApprover(@RequestParam(value = "search", required = false) String kode) {
        ResponseEntity responseEntity = null;
        //log.info(">>> signer : {}", d.toString(kode));
        Map<String, String> listKode = suratService.getJSONUserByRole(kode, "approver");

        responseEntity = ResponseEntity.ok(listKode);
        //log.info(">>> listKode : {}", d.toString(listKode));
        return responseEntity;
    }

    @GetMapping(value = "/json-one-signer/{id}")
    public ResponseEntity getJSONOneSigner(@PathVariable("id") Integer id) {
        ResponseEntity responseEntity = null;
        User user = userMapper.selectByPrimaryKey(id);
        responseEntity = ResponseEntity.ok(user);
        //log.info(">>> listKode : {}", d.toString(user));
        return responseEntity;
    }

    @GetMapping(value = "/buat-surat/{id}")
    public String getSuratEdit(Model uiModel, @PathVariable("id") Integer idSurat,
                               @ModelAttribute("errorText") String errorText, RedirectAttributes attributes) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        BuatSuratCmd surat = new BuatSuratCmd();
        Surat suratById = suratService.getSuratById(idSurat);

        uiModel.addAttribute("errorText", errorText);
        uiModel.addAttribute("headerName", suratService.headerName(suratById));

        //check if status is not draft and logged in user isnt surat maker
        String notifikasi = suratService.validateAccessBuatSurat(idSurat);
        if(!notifikasi.equals("")){
            attributes.addFlashAttribute("ERROR", notifikasi);
            return "redirect:/daftar-riwayat";
        }

        surat = surat.toSuratCmd(suratById);

        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        surat.setTanggalPembuatanStr(df.format(System.currentTimeMillis()));

        List<User> listApprover = userService.getAllUserByRole("approver");
        List<User> listSigner = userService.getAllUserByRole("signer");
        List<SuratSignDTO> selectedApprover;
        List<SuratSignDTO> selectedSigner;

        List<SuratSignDTO> listSelectedApprover = suratApproveService.getListSuratApproveAndUser(idSurat, true);
        List<SuratSignDTO> listSelectedApproverDraft = suratApproveService.getListSuratApproveAndUser(idSurat, null);
        List<SuratSignDTO> listKomentarApprover = suratSignService.getKomentarApprover(idSurat);

        List<SuratSignDTO> listSelectedSigner = suratApproveService.getListSuratSignAndUser(idSurat, true);
        List<SuratSignDTO> listSelectedSignerDraft = suratApproveService.getListSuratSignAndUser(idSurat, null);
        List<SuratSignDTO> listKomentarSigner = suratSignService.getKomentarSigner(idSurat);

        if(suratById.getStatusId() > -1) {
            selectedApprover = listSelectedApprover;
            selectedSigner = listSelectedSigner;
        } else {
            selectedApprover = listSelectedApproverDraft;
            selectedSigner = listSelectedSignerDraft;

        }

        System.out.println(">>> selectedSigner : {}" + d.toString(selectedSigner));

        List<Template> listTemplate = suratService.getAllTemplate();

        uiModel.addAttribute("surat", surat);
        uiModel.addAttribute("listSignerDd",listSigner);
        if(!selectedSigner.isEmpty()){
            User signer = userMapper.selectByPrimaryKey(selectedSigner.get(0).getIdUser());
            uiModel.addAttribute("namaSigner", signer.getNama());
            uiModel.addAttribute("nip", signer.getNip());
            uiModel.addAttribute("jabatanSigner", signer.getJabatan());
        }
        String[] monthNames = {
                "Januari", "Februari", "Maret",
                "April", "Mei", "Juni", "Juli",
                "Agustus", "September", "Oktober",
                "November", "Desember" };
        Date currentDate = new Date(System.currentTimeMillis());
        uiModel.addAttribute("tanggal", currentDate.getDate()+" "+monthNames[currentDate.getMonth()]+" "+(1900+currentDate.getYear()));
        uiModel.addAttribute("cetak", false);
        uiModel.addAttribute("listTemplate",listTemplate);
        uiModel.addAttribute("listKomentarApprover", listKomentarApprover);
        uiModel.addAttribute("listKomentarSigner", listKomentarSigner);
        uiModel.addAttribute("selectedApprover", selectedApprover);
        uiModel.addAttribute("selectedSigner", selectedSigner);
        uiModel.addAttribute("isSaveDraft", true);


        KlasifikasiDetail kodeKlasifikasi = new KlasifikasiDetail();
        if(surat.getIdKodeKlasifikasi()!=null){
            kodeKlasifikasi = suratService.getJSONKodeKlasifikasiById(surat.getIdKodeKlasifikasi());
        }
        uiModel.addAttribute("klasifikasi", kodeKlasifikasi);

        UnitKerjaDetail unitKerjaDetail = new UnitKerjaDetail();
        if(surat.getUnitKerja()!=null){
            unitKerjaDetail = suratService.getJSONUnitKerjaById(surat.getUnitKerja());
        }
        uiModel.addAttribute("unitKerjaDetail", unitKerjaDetail);

        SifatDetail sifatDetail = new SifatDetail();
        if(surat.getSifat()!=null && !surat.getSifat().isEmpty()){
            sifatDetail = suratService.getJSONSifatById(surat.getSifat());
        }
        uiModel.addAttribute("sifatDetail", sifatDetail);

        User kepadaDetail = new User();
        List<User> selectedKepada = new ArrayList<>();
        List<User> selectedTembusan = new ArrayList<>();
        if(!StringUtils.isBlank(surat.getKepada())){
            if(surat.getTipe() != null && surat.getTipe() == 2){
                String[] parts = surat.getKepada().split(",");
                for(String x: parts){
                    kepadaDetail = userMapper.selectByPrimaryKey(Integer.parseInt(x));
                    selectedKepada.add(kepadaDetail);
                }
            }
        }

        if(surat.getTembusan()!=null && !surat.getTembusan().equals("")){
            String[] parts = surat.getTembusan().split(",");
            for(String x: parts){
                kepadaDetail = userMapper.selectByPrimaryKey(Integer.parseInt(x));
                selectedTembusan.add(kepadaDetail);
            }
        }

        uiModel.addAttribute("kepada", selectedKepada);
        uiModel.addAttribute("tembusan", selectedTembusan);

        System.out.println(selectedApprover.size());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());

        uiModel.addAttribute("pembuat",detailUserLogin.getNama());
        return "buat_surat";
    }

    @GetMapping(value = "/hapus-lampiran/{id}")
    public String  hapusLampiran(@PathVariable("id") Integer id) {
        String notifikasi = suratService.validateAccessBuatSurat(id);
        if(notifikasi.equals("")){
            suratService.hapusLampiran(id);
        }
        return "redirect:/buat-surat/"+id;
    }

    @GetMapping(value = "/json-dropdown-kepada/")
    public ResponseEntity getJSONDropdownKepada(@RequestParam(value = "search", required = false) String searchName) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        ResponseEntity responseEntity = null;
        //log.info(">>> jabatan : {}", d.toString(searchName));
        Map<String, String> listUser = suratService.getDropdownUserByUsername(searchName);
        responseEntity = ResponseEntity.ok(listUser);
        //log.info(">>> listUser : {}", d.toString(listUser));
        return responseEntity;
    }

    @GetMapping(value = "/json-unit-kerja/")
    public ResponseEntity getJSONUnitKerja(@RequestParam(value = "search", required = false) String nama) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        ResponseEntity responseEntity = null;
        //log.info(">>> string search : {}", d.toString(nama));
        Map<String, String> listUser = suratService.getJSONUnitKerja(nama);
        responseEntity = ResponseEntity.ok(listUser);
        //log.info(">>> listUser : {}", d.toString(listUser));
        return responseEntity;
    }

    @GetMapping(value = "/json-sifat/")
    public ResponseEntity getJSONSifat(@RequestParam(value = "search", required = false) String nama) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        ResponseEntity responseEntity = null;
        //log.info(">>> string search : {}", d.toString(nama));
        Map<String, String> listSifat = suratService.getJSONSifat(nama);
        responseEntity = ResponseEntity.ok(listSifat);
        //log.info(">>> listSifat : {}", d.toString(listSifat));
        return responseEntity;
    }

    @GetMapping(value = "/preview-surat/{id}")
    public void downloadLampiran (@PathVariable("id") Integer id, HttpServletResponse response) {
        try {
            Surat surat = suratService.getSuratById(id);
            if(surat == null || surat.getId() == null || surat.getTipe() == null) {
                return;
            }

            suratService.generateTempFile(surat);


            response.setHeader ("Content-Disposition", "filename= \""
                    + folderPrefix + "/arsipui/temp/temp_" + surat.getId() + "\"");
            response.setContentType ("application/pdf");


            File file = new File (folderPrefix + "/arsipui/temp/temp_" + surat.getId()+".pdf");

            // download file
            InputStream is = new FileInputStream(file);
            ServletOutputStream out = response.getOutputStream ();
            IOUtils.copy (is, out);
            out.flush ();
            out.close ();
            is.close ();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }
}
