package com.pusilkom.arsipui.controller;

import com.github.dandelion.datatables.core.ajax.DataSet;
import com.github.dandelion.datatables.core.ajax.DatatablesCriterias;
import com.github.dandelion.datatables.core.ajax.DatatablesResponse;
import com.lowagie.text.DocumentException;
import com.pusilkom.arsipui.dto.form.search.DaftarApprovalSearchForm;
import com.pusilkom.arsipui.dto.table.DaftarApprovalItem;
import com.pusilkom.arsipui.dto.table.SignerVerifyDTO;
import com.pusilkom.arsipui.dto.table.SuratDTO;
import com.pusilkom.arsipui.dto.table.SuratSignDTO;
import com.pusilkom.arsipui.dto.view.KlasifikasiDetail;
import com.pusilkom.arsipui.dto.view.PenggunaDetail;
import com.pusilkom.arsipui.dto.view.SifatDetail;
import com.pusilkom.arsipui.model.Surat;
import com.pusilkom.arsipui.model.SuratApprove;
import com.pusilkom.arsipui.model.SuratSign;
import com.pusilkom.arsipui.model.User;
import com.pusilkom.arsipui.model.mapper.SuratMapper;
import com.pusilkom.arsipui.model.mapper.UserMapper;
import com.pusilkom.arsipui.service.*;
import com.pusilkom.arsipui.util.DebugUtil;
//import groovy.transform.Synchronized;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ITF on 8/15/2019.
 */
@Controller
@Secured("ROLE_AUTHORIZED")
public class ParafController {
    @Autowired
    SuratService suratService;
    @Autowired
    SuratSignService suratSignService;
    @Autowired
    UserService userService;
    @Autowired
    UserMapper userMapper;
    @Autowired
    SuratApproveService suratApproveService;
    @Autowired
    DebugUtil d;
    @Autowired
    ParafService parafService;
    @Autowired
    SuratMapper suratMapper;
    @Autowired
    private TemplateEngine templateEngine;
    Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${folder.upload.surat}")
    private String folderUploadSurat;

    @Value("${folder.prefix}")
    private String folderPrefix;

    @GetMapping(value = "/daftar-paraf")
    public String indexGet(@Valid DaftarApprovalSearchForm searchForm, BindingResult result,
                           Model uiModel) {
        if (result.hasErrors()) {
            return "paraf/daftar_paraf";
        }

        Long jmlApprove = parafService.getJumlahHarusApprove();
        uiModel.addAttribute("jmlHarusApprove", jmlApprove);
        uiModel.addAttribute("daftarRiwayatSearchForm", searchForm);

        return "paraf/daftar_paraf";
    }

    @GetMapping(value = "/daftar-ttd")
    public String indexGet2(@Valid DaftarApprovalSearchForm searchForm, BindingResult result,
                           Model uiModel) {
        if (result.hasErrors()) {
            return "paraf/daftar_sign";
        }

        Long jmlSign = parafService.getJumlahHarusSign();
        uiModel.addAttribute("jmlHarusSign", jmlSign);
        uiModel.addAttribute("daftarRiwayatSearchForm", searchForm);

        return "paraf/daftar_sign";
    }

    @GetMapping(value = "/detail-surat-paraf/{id}")
    public String getDetailSurat(Model uiModel, @PathVariable("id") Integer id,  @ModelAttribute("errorText") String errorText, HttpServletResponse respon) throws IOException, DocumentException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        Surat surat = suratService.getSuratById(id);
        SuratDTO suratDTO = suratService.selectSuratDTO(id);

        uiModel.addAttribute("headerName", suratService.headerName(surat));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User pembuat = userMapper.selectByPrimaryKey(surat.getPembuat());

        User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());

        //set field klasifikasi ke api andieni
        KlasifikasiDetail kodeKlasifikasi = new KlasifikasiDetail();
        if(surat.getIdKodeKlasifikasi()!=null){
            kodeKlasifikasi = suratService.getJSONKodeKlasifikasiById(surat.getIdKodeKlasifikasi());
        }
        suratDTO.setKlasifikasi(kodeKlasifikasi.getNama());

        User kepadaDetail2 = new User();
        List<User> selectedKepada = new ArrayList<>();
        if(surat.getKepada()!=null){
            if(surat.getTipe() != null && surat.getTipe() == 2){
                String[] parts = surat.getKepada().split(",");
                for(String x: parts){
                    kepadaDetail2 = userMapper.selectByPrimaryKey(Integer.parseInt(x));
                    selectedKepada.add(kepadaDetail2);
                }
            }
        }

        List<User> selectedTembusan = new ArrayList<>();

        if(surat.getTembusan()!=null && !surat.getTembusan().equals("")){
            String[] parts = surat.getTembusan().split(",");
            for(String x: parts){
                User penggunaDetail = userMapper.selectByPrimaryKey(Integer.parseInt(x));
                selectedTembusan.add(penggunaDetail);
            }
            System.out.println("selectedKepada:"+selectedTembusan.get(0).getNama());
            uiModel.addAttribute("tembusan", selectedTembusan);
        }

        //set field kepada ke api andieni
        PenggunaDetail kepadaDetail = new PenggunaDetail();
        if(surat.getKepada()!=null && surat.getTipe()==2){
            String listNama = suratService.convertListKepadaToString(surat.getKepada());
            surat.setKepada(listNama);
        }

        uiModel.addAttribute("kepada", selectedKepada);

        //set field tembusan ke api andieni
        if(surat.getTembusan()!=null){
            String listNama = suratService.convertListKepadaToString(surat.getTembusan());
            surat.setTembusan(listNama);
        }

        //set field sifat ke api andieni
        SifatDetail sifatDetail = new SifatDetail();
        if(surat.getSifat()!=null && !surat.getSifat().isEmpty()) sifatDetail = suratService.getJSONSifatById(surat.getSifat());
        if(sifatDetail.getId() != null) {
            surat.setSifat(suratService.convert(sifatDetail.getNama()));
        } else {
            surat.setSifat("-");
        }

        SignerVerifyDTO signerVerifyDTO = new SignerVerifyDTO();
        //signerVerifyDTO.setNik("3271030105950011");
        signerVerifyDTO.setNik(detailUserLogin.getNik());
        signerVerifyDTO.setPassphrase("twentyone");

        //suratService.kirimBsre(surat, signerVerifyDTO, respon);

        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy ");

        String tanggal = "";
        if(surat.getTanggalPembuatan()!= null)
            tanggal = format.format(surat.getTanggalPembuatan());

        List<SuratApprove> listApprove = suratApproveService.getListSuratApproveByIdSurat(surat.getId());
        List<SuratApprove> listApproveAllFlag = suratApproveService.getListSuratApproveByIdSuratAllFlag(surat.getId());
        List<SuratSign> listSign = suratSignService.getListSuratSignByIdSurat(surat.getId());
        List<SuratSign> listSignAllFlag = suratSignService.getListSuratSignByIdSuratAllFlag(surat.getId());

        List<SuratSignDTO> listApproveDTO = suratSignService.getListSuratApproveDTO(surat.getId());
        List<SuratSignDTO> listKomentarApprover = suratSignService.getListKomentarApprover(surat.getId());
        List<SuratSignDTO> listKomentarSigner = suratSignService.getListKomentarSigner(surat.getId());
        List<SuratSignDTO> listSignDTO = suratSignService.getListSuratSignDTO(surat.getId());

        Boolean roleSigner = false;
        Boolean roleApprover = false;
        Boolean roleProducer = false;
        int idUserLogin = detailUserLogin.getId();
        try {
            ////log.info(">>> surat id : " + surat.getId());
            ////log.info(">>> surat dibuat oleh : " + surat.getPembuat());
            ////log.info(">>> surat ditandatangani oleh : " + listSign.size());
            ////log.info(">>> surat diapprove oleh : " + listApprove.size());
            ////log.info(">>> user yang login : " + idUserLogin + " - " + detailUserLogin.getUsername());
        }catch(Exception e){
            e.printStackTrace();
        }

        //check role user yang login
        if(idUserLogin == surat.getPembuat()){
            roleProducer = true;
        }
        //approver
        if(listApprove.size() > 0){
            for(int i = 0; i< listApprove.size(); i++){
                ////log.info(">>>> id approve yang ke - " + i + " : " + listApprove.get(i).getIdUser());
                if(idUserLogin == listApprove.get(i).getIdUser()){
                    roleApprover = true;
                    break;
                }
            }
        }

        //signer
        if(listSign.size() > 0){
            for(int i = 0; i< listSign.size(); i++){
                ////log.info(">>>> id signer yang ke - " + i + " : " + listSign.get(i).getIdUser() + "komentar ==>" + listSign.get(i).getKomentar());
                if(idUserLogin == listSign.get(i).getIdUser()){
                    roleSigner = true;
                    break;
                }
            }
        }

        //log.info(">>>> roleProducer " + roleProducer);
        //log.info(">>>> roleSigner " + roleSigner);
        //log.info(">>>> roleApprover " + roleApprover);


        //penjagaan dibagian status approve / tidak approve
        SuratApprove approve = new SuratApprove();
        if(roleApprover == true)
            approve = suratApproveService.getApproveByIdSuratIdUser(id, idUserLogin);

        //penjagaan dibagian status sign / tidak sign
        SuratSign sign = new SuratSign();
        if(roleSigner == true)
            sign = suratSignService.getSuratSignByIdSuratIdUser(id, idUserLogin);

        if(roleSigner && surat.getStatusId() == 2 && sign.getStatus() == null){
            //uiModel.addAttribute("textError", null);
            uiModel.addAttribute("signerVerification", new SignerVerifyDTO());
            uiModel.addAttribute("errorText", errorText);
        }

        List<SuratSignDTO> signers = suratSignService.getListSuratSignDTO(surat.getId());
        User signer = userMapper.selectByPrimaryKey(signers.get(0).getIdUser());
        uiModel.addAttribute("namaSigner", signer.getNama());
        uiModel.addAttribute("jabatanSigner", signer.getJabatan());
        uiModel.addAttribute("nip", signer.getNip());


        DateFormat df = new SimpleDateFormat("hh:mm:ss aa");

        uiModel.addAttribute("pembuat",pembuat.getNama());

        String[] monthNames = {
                "Januari", "Februari", "Maret",
                "April", "Mei", "Juni", "Juli",
                "Agustus", "September", "Oktober",
                "November", "Desember" };
        uiModel.addAttribute("tanggal", surat.getTanggalPembuatan().getDate()+" "+monthNames[surat.getTanggalPembuatan().getMonth()]+" "+(1900+surat.getTanggalPembuatan().getYear()));

        uiModel.addAttribute("cetak", false);
        uiModel.addAttribute("tanggal2",surat.getTanggalPembuatan().getDate()+" "+monthNames[surat.getTanggalPembuatan().getMonth()]+" "+(1900+surat.getTanggalPembuatan().getYear()) + " " + df.format(surat.getTanggalPembuatan()));
        uiModel.addAttribute("roleSigner",roleSigner);
        uiModel.addAttribute("roleApprover",roleApprover);
        uiModel.addAttribute("roleProducer",roleProducer);

        if(surat.getLampiran() == null){
            surat.setLampiran("-");
        }
        uiModel.addAttribute("surat",surat);
        uiModel.addAttribute("approve",approve);
        uiModel.addAttribute("sign",sign);
        uiModel.addAttribute("listApprove",listApproveDTO);
        uiModel.addAttribute("listSign",listSignDTO);
        uiModel.addAttribute("suratDTO",suratDTO);
        uiModel.addAttribute("listKomentarApprover", listKomentarApprover);
        uiModel.addAttribute("listKomentarSigner", listKomentarSigner);
        uiModel.addAttribute("user",detailUserLogin);

        if(roleApprover == true || roleProducer == true || roleSigner == true) {
            return "paraf/paraf-surat";
        }else {
            //approver
            if(listApproveAllFlag.size() > 0){
                for(int i = 0; i< listApproveAllFlag.size(); i++){
                    if(idUserLogin == listApproveAllFlag.get(i).getIdUser() && listApproveAllFlag.get(i).getStatus() != null){
                        return "paraf/paraf-surat";
                    }
                }
            }

            //signer
            if(listSignAllFlag.size() > 0){
                for(int i = 0; i< listSignAllFlag.size(); i++){
                    if(idUserLogin == listSignAllFlag.get(i).getIdUser() && listSignAllFlag.get(i).getStatus() != null){
                        return "paraf/paraf-surat";
                    }
                }
            }
            return "error/403";
        }
    }

    @RequestMapping(value = "/detail-surat-paraf/setuju", method = RequestMethod.POST, params = "action=setuju")
    public String saveParafSetuju(@ModelAttribute("surat") SuratApprove approve, final BindingResult result, Model model) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());
            if(suratService.getSuratById(approve.getIdSurat()).getStatusId() > 0) {
                return "redirect:/detail-surat-paraf/" + approve.getIdSurat();
            }

            //set tidak setuju di approve
            suratApproveService.saveSetuju(approve, detailUserLogin.getId());
            //log.info(">>>> isi komentar : " + approve.getKomentar());
            //update jumlah setuju di t_surat
            suratService.updateJumlahApproved(approve.getIdSurat());
            //keluarin jumlah yang uda respon dr t_surat_approve -- sampai jumlah yang != null sama dengan jumlah approval
            //set status di t_surat
            suratApproveService.updateStatusSuratAfterApproved(approve.getIdSurat());
            //log.info(">>>> disetujui " + approve.getIdSurat() + "; user Login : " + detailUserLogin.getId());

        return "redirect:/detail-surat-paraf/" + approve.getIdSurat();
    }

    @RequestMapping(value = "/detail-surat-paraf/setuju", method = RequestMethod.POST, params = "action=tidakSetuju")
    public String saveParafTidakSetuju(@ModelAttribute("surat") SuratApprove approve,  @ModelAttribute("signerVerification") SignerVerifyDTO signer, final BindingResult result, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());
        if(suratService.getSuratById(approve.getIdSurat()).getStatusId() > 0) {
            return "redirect:/detail-surat-paraf/" + approve.getIdSurat();
        }

        //set tidak setuju di approve
        suratApproveService.saveTidakSetuju(approve, detailUserLogin.getId());
        //keluarin jumlah yang uda respon dr t_surat_approve -- sampai jumlah yang != null sama dengan jumlah approval
        //set status di t_surat
        suratApproveService.updateStatusSuratAfterApproved(approve.getIdSurat());
        //log.info(">>>> tidak disetujui " + approve.getIdSurat() + "; user Login : " + detailUserLogin.getId());

        return "redirect:/detail-surat-paraf/" + approve.getIdSurat();
    }

    @RequestMapping(value = "/detail-surat-paraf/tandatangan", method = RequestMethod.POST, params = "action=setuju")
    public String saveParafTandaTangan(@ModelAttribute("surat") SuratSign sign, @ModelAttribute("signerVerification") SignerVerifyDTO signer, Model model,  RedirectAttributes attributes) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException, ParseException {
        System.out.println(signer.getNik()+" "+signer.getPassphrase()+" "+sign.getIdSurat());
        //log.info(">>> searchForm : {}", d.toString(signer));
        //log.info(">>> isi NIK : {}", signer.getNik());

        if(signer.getPassphrase().isEmpty()){
            attributes.addAttribute("errorText", "Mohon Isi Passphrase");
        } else {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());

            Surat detailSurat = suratService.getSuratById(sign.getIdSurat());
            if(detailSurat.getStatusId() > 2) {
                return "redirect:/detail-surat-paraf/" + sign.getIdSurat();
            }
            System.out.println("TTTT "+signer.getNik()+" "+signer.getPassphrase());
            try{
                suratSignService.sign(detailSurat, sign, signer, detailUserLogin);
                //log.info(">>>> ditandatangani " + sign.getIdSurat() + "; user Login : " + detailUserLogin.getId());
            } catch (Exception e) {
                //log.info(">>> detail exception : ", e);
                if(suratService.getSuratById(sign.getIdSurat()).getStatusId() < 5) {
                    String preview = suratService.previewNoSurat(detailSurat.getTipe(), detailUserLogin.getId(), detailSurat.getIdKodeKlasifikasi(), false, "");
                    suratService.updateNomorSurat(sign.getIdSurat(), preview);
                }
                e.printStackTrace();
                attributes.addAttribute("errorText", e.getMessage());
                return "redirect:/detail-surat-paraf/" + sign.getIdSurat();
            }
        }
//        if(signer.getNik().equals("3271030105950011") && signer.getPassphrase().equals("twentyone")) {
        return "redirect:/detail-surat-paraf/" + sign.getIdSurat();
    }

    @RequestMapping(value = "/detail-surat-paraf/tandatangan", method = RequestMethod.POST, params = "action=tidakSetuju")
    public String saveParafTidakTidakTandaTangan(@ModelAttribute("surat") SuratSign sign, @ModelAttribute("signerVerification") SignerVerifyDTO signer,  RedirectAttributes attributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());

        if(suratService.getSuratById(sign.getIdSurat()).getStatusId() > 2) {
            return "redirect:/detail-surat-paraf/" + sign.getIdSurat();
        }
        //set tidak setuju di approve
        suratSignService.saveTidakTandaTangan(sign, detailUserLogin.getId());
        //keluarin jumlah yang uda respon dr t_surat_approve -- sampai jumlah yang != null sama dengan jumlah approval
        //set status di t_surat
        suratSignService.updateStatusSuratAfterSigned(sign.getIdSurat());
        //log.info(">>>> tidak ditandatangani " + sign.getIdSurat() + "; user Login : " + detailUserLogin.getId());

        return "redirect:/detail-surat-paraf/" + sign.getIdSurat();
    }

    @RequestMapping(value = "/daftar-paraf", method = RequestMethod.POST)
    @ResponseBody
    public DatatablesResponse<DaftarApprovalItem> postTableSearch(DaftarApprovalSearchForm searchForm, HttpServletRequest httpServletRequest) {
        DatatablesCriterias criterias = DatatablesCriterias.getFromRequest(httpServletRequest);
        searchForm.setCriterias(criterias);

        DataSet<DaftarApprovalItem> dataSet = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());
            searchForm.setUserId(detailUserLogin.getId());
            //log.info(">>> searchForm : {}", d.toString(searchForm));
            dataSet = parafService.getDataSetApproval(searchForm);
        } catch (Exception e) {
            log.error("TABLE SURAT : ", e);

        }

        return DatatablesResponse.build(dataSet, criterias);
    }

    @RequestMapping(value = "/daftar-ttd", method = RequestMethod.POST)
    @ResponseBody
    public DatatablesResponse<DaftarApprovalItem> postTableSearchSign(DaftarApprovalSearchForm searchForm, HttpServletRequest httpServletRequest) {
        DatatablesCriterias criterias = DatatablesCriterias.getFromRequest(httpServletRequest);
        searchForm.setCriterias(criterias);

        DataSet<DaftarApprovalItem> dataSet = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());
            searchForm.setUserId(detailUserLogin.getId());
            //log.info(">>> searchForm : {}", d.toString(searchForm));
            dataSet = parafService.getDataSetSign(searchForm);
        } catch (Exception e) {
            log.error("TABLE SURAT : ", e);

        }

        return DatatablesResponse.build(dataSet, criterias);
    }

    @GetMapping(value = "/download-lampiran/{id}")
    public void downloadLampiran (@PathVariable("id") Integer id, HttpServletResponse response) {
        try {
            Surat surat = suratService.getSuratById(id);
            String filename = surat.getFile();

            response.setHeader ("Content-Disposition", "filename= \""
                    + surat.getId()+"_"+filename + "\"");
            response.setContentType ("application/pdf");

            //File file = new File (path + "\\" + surat.getPembuat()+"\\"+surat.getId()+"_"+filename);
            //File file = new File ("template/Naskah Dinas.pdf");
            File file = new File (folderUploadSurat+"/"+surat.getPembuat()+"/"+surat.getId()+"_"+filename);
            // download file
            //response.setContentType ("application/pdf,application/*");
            InputStream is = new FileInputStream(file);
            ServletOutputStream out = response.getOutputStream ();
            IOUtils.copy (is, out);
            out.flush ();
            out.close ();
            is.close ();

            //log.info(">>> " +surat.getId()+"_"+filename);
            //log.info(">>> " +folderUploadSurat+"/"+surat.getPembuat()+"/"+surat.getId()+"_"+filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }



    @GetMapping(value = "/download-verified/{id}")
    public void downloadVerified (@PathVariable("id") Integer id, HttpServletResponse response) {
        try {
            Surat surat = suratService.getSuratById(id);
            String filename = surat.getFile();
            String path = surat.getDirektori();

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());
            response.setHeader("Content-Disposition", "filename= \""
                    + "verified" + "_" + surat.getId()+".pdf");
            response.setContentType("application/pdf");
            File file = new File (folderPrefix+"/arsipui/verified/"+"verified_"+surat.getId()+".pdf");

            InputStream is = new FileInputStream(file);
            ServletOutputStream out = response.getOutputStream();
            IOUtils.copy(is, out);
            out.flush();
            out.close();
            is.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }


    @GetMapping(value = "/daftar-bsre")
    public String indexGet3(@Valid DaftarApprovalSearchForm searchForm, BindingResult result,
                            Model uiModel) {
        if (result.hasErrors()) {
            return "paraf/daftar_bsre";
        }

        uiModel.addAttribute("daftarRiwayatSearchForm", searchForm);

        return "paraf/daftar_bsre";
    }

    @RequestMapping(value = "/daftar-bsre", method = RequestMethod.POST)
    @ResponseBody
    public DatatablesResponse<DaftarApprovalItem> postTableSearchBsre(DaftarApprovalSearchForm searchForm, HttpServletRequest httpServletRequest) {
        DatatablesCriterias criterias = DatatablesCriterias.getFromRequest(httpServletRequest);
        searchForm.setCriterias(criterias);

        DataSet<DaftarApprovalItem> dataSet = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());
            searchForm.setUserId(detailUserLogin.getId());
            //log.info(">>> searchForm : {}", d.toString(searchForm));
            dataSet = parafService.getDataSetBsre(searchForm);
        } catch (Exception e) {
            log.error("TABLE SURAT : ", e);

        }
        return DatatablesResponse.build(dataSet, criterias);
    }

    @RequestMapping(value = "/detail-surat-paraf/edit", method = RequestMethod.POST, params = "action=edit")
    public String editSurat(@ModelAttribute("surat") Surat surat, final BindingResult result, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User detailUserLogin = userService.getUserDetailByUsername(((UserDetails) authentication.getPrincipal()).getUsername());
        Surat suratGet = suratService.getSuratById(surat.getId());
        if(suratGet.getStatusId() == 3 || suratGet.getStatusId() == 1) {
            //set status surat menjadi -1 (draft)
            suratService.updateStatusSuratEdit(surat.getId());
            return "redirect:/buat-surat/" + surat.getId();
        }

        if(suratGet.getStatusId() == -1) {
            return "redirect:/buat-surat/" + surat.getId();
        }
        return "redirect:/daftar-riwayat";
    }
}
