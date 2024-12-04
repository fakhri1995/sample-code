package com.pusilkom.arsipui.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dandelion.datatables.core.ajax.DataSet;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.pusilkom.arsipui.dto.form.cmd.BuatSuratCmd;
import com.pusilkom.arsipui.dto.form.search.DaftarRiwayatSearchForm;
import com.pusilkom.arsipui.dto.table.DaftarRiwayatItem;
import com.pusilkom.arsipui.dto.table.SignerVerifyDTO;
import com.pusilkom.arsipui.dto.table.SuratDTO;
import com.pusilkom.arsipui.dto.table.SuratSignDTO;
import com.pusilkom.arsipui.dto.view.*;
import com.pusilkom.arsipui.model.*;
import com.pusilkom.arsipui.model.mapper.*;
import com.pusilkom.arsipui.util.DebugUtil;
import com.pusilkom.arsipui.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.UriUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by fahri on 1/7/17.
 */


@Service
@Transactional(readOnly = true)
public class SuratService {

    Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    DebugUtil d;

    public static Map<String, String> abbrevFakultasKop  = new HashMap<String, String>() {{
        put("FK", "Fakultas Kedokteran");
        put("FKG", "Fakultas Kedokteran Gigi");
        put("FMIPA", "Fakultas Matematika dan Ilmu Pengetahuan Alam ");
        put("FT", "Fakultas Teknik");
        put("FH", "Fakultas Hukum");
        put("FEB", "Fakultas Ekonomi dan Bisnis");
        put("FIB", "Fakultas Ilmu Pengetahuan Budaya");
        put("FPSIKOLOGI", "Fakultas Psikologi");
        put("FISIP", "Fakultas Ilmu Sosial dan Ilmu Politik");
        put("FKM", "Fakultas Kesehatan Masyarakat");
        put("FIK", "Fakultas Ilmu Keperawatan");
        put("FF", "Fakultas Farmasi");
        put("FIA", "Fakultas Ilmu Administrasi");
        put("FASILKOM", "Fakultas Ilmu Komputer");
    }};

    public static String[] abbrevSekolah = {"SKSG", "SIL", "Sekolah", "DRRC"};

    @Autowired
    TemplateMapper templateMapper;

    @Autowired
    SuratMapper suratMapper;

    @Autowired
    SuratDTOMapper suratDTOMapper;

    @Autowired
    SuratApproveMapper suratApproveMapper;

    @Autowired
    SuratSignMapper suratSignMapper;

    @Autowired
    KodeKlasifikasiMapper kodeKlasifikasiMapper;

    @Autowired
    OrganisasiMapper organisasiMapper;

    @Autowired
    UserMapper userMapper;

    @Autowired
    UserService userService;

    @Autowired
    TemplateEngine templateEngine;

    @Autowired
    SuratApproveService suratApproveService;

    @Autowired
    SuratSignService suratSignService;

    @Value("${folder.upload.surat}")
    private String folderUploadSurat;

    @Value("${url.service}")
    private String urlService;

    @Value("${dump.command}")
    private String dump;

    @Value("${folder.prefix}")
    private String folderPrefix;

    @Transactional(readOnly = false)
    public List<Template> getAllTemplate() {
        TemplateExample ex = new TemplateExample();
        return templateMapper.selectByExample(ex);
    }




    @Transactional(readOnly = false)
    public String headerName(Surat surat) {
        User user = null;
        if(surat.getPembuat() != null) {
             user = userMapper.selectByPrimaryKey(surat.getPembuat());
        } else {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            user = userService.getUserDetailByUsername(authentication.getName());
        }
        String initial = user.getInitial().trim().toLowerCase();
        String[] splitSpace = user.getInitial().toLowerCase().split(" ");
        String lastWord = splitSpace[splitSpace.length -1].toUpperCase();
        log.info("split space " + splitSpace.length);
        if(user.getInitial().toLowerCase().contains("fakultas")){
            String[] splitFakultas = user.getInitial().toLowerCase().split("fakultas");
            return ("fakultas" + splitFakultas[splitFakultas.length -1]).replaceAll("\\s+","") + ".png";
        } else if(splitSpace.length > 0 && abbrevFakultasKop.get(splitSpace[splitSpace.length -1].toUpperCase()) != null) {
            return abbrevFakultasKop.get(splitSpace[splitSpace.length -1].toUpperCase()).toLowerCase().replaceAll("\\s+","") + ".png";
        } else if(user.getInitial().trim().toLowerCase().equals("sekolah kajian stratejik dan global") || splitSpace[splitSpace.length -1].toUpperCase().equals(abbrevSekolah[0])) {
            return "sksg.png";
        } else if(user.getInitial().trim().toLowerCase().equals("sekolah ilmu lingkungan")
            || splitSpace[splitSpace.length -1].toUpperCase().equals(abbrevSekolah[1])) {
            return "sil.png";
        } else if(user.getInitial().trim().toLowerCase().equals("sekolah".trim().toLowerCase())
            || splitSpace[splitSpace.length -1].toUpperCase().equalsIgnoreCase(abbrevSekolah[2].toUpperCase())) {
            return "sekolah.png";
        } else if(user.getInitial().trim().toLowerCase().equals("disaster risk reduction center".trim().toLowerCase())
            || splitSpace[splitSpace.length -1].toUpperCase().equalsIgnoreCase(abbrevSekolah[3].toUpperCase())) {
            return "drrc.png";
        } else if(user.getInitial().toLowerCase().contains("vokasi")) {
            if(user.getInitial().toLowerCase().equals("lembaga vokasi program pendidikan vokasi")
                    || user.getInitial().toLowerCase().equals("lembaga sertifikasi profesi program pendidikan vokasi")) {
                return user.getInitial().toLowerCase().replaceAll("\\s+","") + ".png";
            } else {
                return "vokasi.png";
            }
        } else if(initial.equals("Lembaga Pendidikan Lanjutan Ilmu Hukum".toLowerCase()) ||
            initial.equals("The Center For Continuing Legal Education".toLowerCase()) || lastWord.equals("LPLIH") ||
            initial.equals("CLE")){
            return "cle.png";
        } else {
            return "pau.png";
        }
    }

    private void replaceFile(Surat surat, MultipartFile file) throws IOException {
        File oldFile = new File (folderUploadSurat+"/"+surat.getPembuat()+"/"+surat.getId()+"_"+surat.getFile());
        File convFile = new File(file.getOriginalFilename());
        boolean isTwoEqual = FileUtils.contentEquals(oldFile, convFile);
        if(!isTwoEqual) oldFile.delete();
    }

    private void saveFile(Surat surat, MultipartFile file, Integer usrId, Integer suratId) throws MimeTypeException, IOException {
        if(surat != null && surat.getPembuat() != null){
            usrId = surat.getPembuat();
        }
        File fileFolder = createFileFolder("" + usrId);
        TikaConfig config = TikaConfig.getDefaultConfig();
        com.google.common.net.MediaType mediaType = FileUtil.getMediaTypeBeforeDownload(file);
        String saveFilename = suratId + "_" + file.getOriginalFilename().replace(" ", "_");
        BufferedOutputStream outStream = null;

        File fileDestination = new File(fileFolder, saveFilename);
        outStream = new BufferedOutputStream(new FileOutputStream(fileDestination));
        FileCopyUtils.copy(file.getInputStream(), outStream);
        outStream.close();
    }

    @Transactional(readOnly = false)
    public Integer saveCmdOnKoreksi(BuatSuratCmd buatSuratCmd, MultipartFile file) throws ParseException, MimeTypeException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        List<SuratSign> listSuratSignerDraft = suratSignService.getListSuratSignByCertainCases(buatSuratCmd.getId(), true);
        Integer idSigner = listSuratSignerDraft.get(0).getIdUser();

        String generateNoSurat = "";
        if (buatSuratCmd.getTipe() != null && buatSuratCmd.getIdKodeKlasifikasi() != null && idSigner != -1) {
            generateNoSurat = this.previewNoSurat(buatSuratCmd.getTipe(), idSigner, buatSuratCmd.getIdKodeKlasifikasi(), false, "");
            buatSuratCmd.setNoSurat(generateNoSurat);
        }

        buatSuratCmd.setDirektori(folderUploadSurat);

        Surat surat = this.getSuratById(buatSuratCmd.getId());

        if(needUpdateLampiran(buatSuratCmd, surat)) {
            if (file != null && !file.isEmpty() && surat != null && surat.getFile() != null) {
                replaceFile(surat, file);
            }

            if (file != null && !file.isEmpty()) {
                saveFile(surat, file, surat.getPembuat(), surat.getId());
            }
        } else {
            buatSuratCmd.setFile(null);
        }

        if(buatSuratCmd.getTembusan() == null) {
            buatSuratCmd.setTembusan("");
        }

        Surat toSurat = buatSuratCmd.toSurat();
        return this.save(toSurat);
    }

    private Boolean needUpdateLampiran(BuatSuratCmd buatSuratCmd, Surat surat) {
        return buatSuratCmd.getFile() != null && !buatSuratCmd.getFile().equals("");
    }

    @Transactional(readOnly = false)
    public Integer saveCmd(BuatSuratCmd buatSuratCmd, MultipartFile file, Boolean isSaveDraft) throws ParseException, MimeTypeException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User detailUserLogin = userService.getUserDetailByUsername(authentication.getName());

        if(buatSuratCmd.getId() == null) {
            buatSuratCmd.setPembuat(detailUserLogin.getId());
        }



        int idSigner = -1;
        Surat surat = this.getSuratById(buatSuratCmd.getId());

        if (buatSuratCmd.getListSigner() != null) {
            buatSuratCmd.setJumlahSignee(buatSuratCmd.getListSigner().size());
            idSigner = Integer.parseInt(buatSuratCmd.getListSigner().get(0));
        }
        if (buatSuratCmd.getListApprover() != null) {
            buatSuratCmd.setJumlahApproval(buatSuratCmd.getListApprover().size());
        } else {
            buatSuratCmd.setJumlahApproval(0);
        }

        if(isSaveDraft){
            buatSuratCmd.setStatusId(-1);
        } else if(!isSaveDraft && (buatSuratCmd.getJumlahApproval() == null || buatSuratCmd.getJumlahApproval() ==  0)){
            buatSuratCmd.setStatusId(2);
        } else {
            buatSuratCmd.setStatusId(0);

        }

        String generateNoSurat = "";
        if (buatSuratCmd.getTipe() != null && buatSuratCmd.getIdKodeKlasifikasi() != null && idSigner != -1) {
            generateNoSurat = this.previewNoSurat(buatSuratCmd.getTipe(), idSigner, buatSuratCmd.getIdKodeKlasifikasi(), false, "");
            buatSuratCmd.setNoSurat(generateNoSurat);
        }

        buatSuratCmd.setJumlahApproved(0);
        buatSuratCmd.setJumlahSigned(0);


        buatSuratCmd.setDirektori(folderUploadSurat);

        //log.info(">>> toSurat : {}", d.toString(toSurat));

        if(needUpdateLampiran(buatSuratCmd, surat)) {
            //replace old file if there is new file
            if (file != null && !file.isEmpty() && surat != null && surat.getFile() != null) {
                replaceFile(surat, file);
            }
        } else {
            buatSuratCmd.setFile(null);
        }

        if(buatSuratCmd.getTembusan() == null) {
            buatSuratCmd.setTembusan("");
        }

        Surat toSurat = buatSuratCmd.toSurat();
        Integer suratId = this.save(toSurat);
        if(surat != null) {
            suratId = surat.getId();
        }

        if(needUpdateLampiran(buatSuratCmd, surat)) {
            //save file
            if (file != null && !file.isEmpty()) {
                saveFile(surat, file, detailUserLogin.getId(), suratId);
            }
        }



        try {
            //get list drafted old approver
            List<SuratApprove> listSuratApproveDraft = suratApproveService.getListSuratApproveByCertainCases(suratId, null);

            // delete previously saved approvers
            if (listSuratApproveDraft != null && listSuratApproveDraft.size() > 0) {
                for (SuratApprove approver : listSuratApproveDraft) {
                    suratApproveMapper.deleteByPrimaryKey(approver.getId());
                }
            }

            // enter new approvers
            if (buatSuratCmd.getListApprover() != null && buatSuratCmd.getListApprover().size() > 0) {
                for (String idApproverBaru : buatSuratCmd.getListApprover()) {
                    SuratApprove suratApprove = new SuratApprove();
                    suratApprove.setIdSurat(suratId);
                    suratApprove.setIdUser(Integer.parseInt(idApproverBaru));
                    if (!isSaveDraft) {
                        suratApprove.setFlag(true);
                    }
                    suratApproveMapper.insert(suratApprove);

                }
            }

            //get list drafted old signer
            List<SuratSign> listSuratSignerDraft = suratSignService.getListSuratSignByCertainCases(suratId, null);

            // delete previously saved signers
            if (listSuratSignerDraft != null && listSuratSignerDraft.size() > 0) {
                for (SuratSign signer : listSuratSignerDraft) {
                    suratSignMapper.deleteByPrimaryKey(signer.getId());
                }
            }

            // enter new signers
            if (buatSuratCmd.getListSigner() != null && buatSuratCmd.getListSigner().size() > 0) {
                for (String signer : buatSuratCmd.getListSigner()) {
                    SuratSign suratSign = new SuratSign();
                    suratSign.setIdSurat(suratId);
                    suratSign.setIdUser(Integer.parseInt(signer));
                    if (!isSaveDraft) {
                        suratSign.setFlag(true);
                    }
                    suratSignMapper.insert(suratSign);
                }
            }
        } catch (NumberFormatException e) {
            log.error("Parse Integer Error : ", e);
        }

        return suratId;

    }

    @Transactional(readOnly = false)
    public Template selectTemplateByPrimaryKey(Integer id) {
        //log.info(">>> ID Template : {}", d.toString(id));
        TemplateExample ex = new TemplateExample();
        ex.createCriteria().andIdEqualTo(id);
        return templateMapper.selectByExample(ex).get(0);
    }

    @Transactional(readOnly = true)
    public Surat getSuratById(Integer id) {
        Surat surat = new Surat();
        surat = suratMapper.getSuratById(id);
        return surat;
    }

    public SuratDTO selectSuratDTO(Integer id) {
        SuratDTO suratDTO = new SuratDTO();
        suratDTO = suratDTOMapper.selectSuratDTO(id);
        return suratDTO;
    }

    public DataSet<DaftarRiwayatItem> getDataSet(DaftarRiwayatSearchForm searchForm) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        List<DaftarRiwayatItem> listItem = suratMapper.getDaftarRiwayatItemBySearchForm(searchForm);
        //log.info(">>> daftar riwayat : {}", d.toString(listItem));
        for (DaftarRiwayatItem item : listItem) {
            if (item.getTanggalPembuatan() != null) {
                SimpleDateFormat pattern = new SimpleDateFormat("dd-MM-yyyy");
                item.setTanggalPembuatanStr(pattern.format(item.getTanggalPembuatan()));
            }

            if(item.getKepada()!= null && !item.getKepada().equals("")) {
                if(item.getTipe() != null && item.getTipe()==2){
                    String listNama = this.convertListKepadaToString(item.getKepada());
                    item.setKepada(listNama);
                }
            }
        }
        Long totalItemFiltered = suratMapper.getTotalDaftarRiwayatItemBySearchForm(searchForm);
        Long totalItem = totalItemFiltered;

        return new DataSet<DaftarRiwayatItem>(listItem, totalItem, totalItemFiltered);
    }

    public File createFileFolder(String fileIdentifier) {
        File fileFolder = new File(
                folderUploadSurat + File.separator + fileIdentifier
                        + File.separator);

        if (!fileFolder.exists())
            fileFolder.mkdirs();

        return fileFolder;
    }

    @Transactional(readOnly = false)
    public void updateJumlahApproved(Integer idSurat) {
        Surat surat = suratMapper.getSuratById(idSurat);
        int jumlahApproved;
        if (surat != null) {
            if (surat.getJumlahApproved() == null) {
                jumlahApproved = 1;
            } else {
                jumlahApproved = surat.getJumlahApproved() + 1;
            }
            surat.setJumlahApproved(jumlahApproved);
            suratMapper.updateByPrimaryKey(surat);
        }
    }

    @Transactional(readOnly = false)
    public void updateJumlahSigned(Integer idSurat) {
        Surat surat = suratMapper.getSuratById(idSurat);
        int jumlahSigned;
        if (surat != null) {
            if (surat.getJumlahSigned() == null) {
                jumlahSigned = 1;
            } else {
                jumlahSigned = surat.getJumlahSigned() + 1;
            }
            surat.setJumlahSigned(jumlahSigned);
            suratMapper.updateByPrimaryKey(surat);
        }
    }



    @Transactional(readOnly = false)
    public String previewNoSurat(Integer idTipe, Integer idSigner, Integer idKodeKlasifikasi, boolean isFinal, String existedCounter) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        TemplateExample tempEx = new TemplateExample();
        tempEx.createCriteria().andIdEqualTo(idTipe);
        Template template = templateMapper.selectByExample(tempEx).get(0);

        UserExample userEx = new UserExample();
        userEx.createCriteria().andIdEqualTo(idSigner);
        User signer = userMapper.selectByExample(userEx).get(0);

        KlasifikasiDetail kodeKlasifikasi = this.getJSONKodeKlasifikasiById(idKodeKlasifikasi);

        String counter = (existedCounter.equals("")) ? "XX" : existedCounter;
        if (isFinal) {
            if (existedCounter.equals("")) {
                if (idTipe == 1) {
                    counter = "" + (signer.getCounterSurat() + 1);
                } else if (idTipe == 2) {
                    counter = "" + (signer.getCounterNota() + 1);
                }
            }
        }
        String preview = template.getInitial() + ".e-" + counter + "/UN2." + signer.getUnit() + "/" + kodeKlasifikasi.getDesc() + "/" + year;
        return preview;
    }

    @Transactional(readOnly = false)
    public Map<String, String> getJSONKodeKlasifikasi(String kode) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String uri = urlService+"/rest/klasifikasi_kegiatan/";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(getRequestFactory());
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic Y2lwdGFuYXNrYWg6cHVzaWxrb20yMDIw");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        //log.info(">>> getresult : {}", d.toString(res.getBody()));

        ObjectMapper mapper = new ObjectMapper();
        List<KlasifikasiDetail> participantJsonList = mapper.readValue(res.getBody(), new TypeReference<List<KlasifikasiDetail>>(){});

        HashMap<String, String> mapList = new HashMap<>();
        for (KlasifikasiDetail obj : participantJsonList) {
            if(obj.getNama().toLowerCase().contains(kode.toLowerCase())){
                mapList.put(String.valueOf(obj.getId()), obj.getNama());
            }
        }

        return mapList;
    }

    @Transactional(readOnly = false)
    public KlasifikasiDetail getJSONKodeKlasifikasiById(Integer id) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String uri = urlService+"/rest/klasifikasi_kegiatan/" + id;
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(getRequestFactory());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic Y2lwdGFuYXNrYWg6cHVzaWxrb20yMDIw");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        //log.info(">>> getresult : {}", d.toString(res.getBody()));

        ObjectMapper mapper = new ObjectMapper();
        KlasifikasiDetail participantJsonList = mapper.readValue(res.getBody(), new TypeReference<KlasifikasiDetail>(){});

        return participantJsonList;
    }

    @Transactional(readOnly = false)
    public Map<String, String> getJSONOrganisasi(String kode) {
        String newStr = "";
        if (kode != null) newStr = "%" + kode + "%";
        OrganisasiExample ex = new OrganisasiExample();
        ex.createCriteria().andNamaLikeInsensitive(newStr);
        List<Organisasi> listKode = organisasiMapper.selectByExample(ex);
        HashMap<String, String> map = new HashMap<>();

        for (Organisasi kk : listKode) {
            map.put(String.valueOf(kk.getId()), kk.getNama());
        }
        return map;
    }

    @Transactional(readOnly = false)
    public Map<String, String> getJSONUserByRole(String kode, String role) {
        String newStr = "";
        if (kode != null) newStr = "%" + kode + "%";
        UserExample ex = new UserExample();
        ex.createCriteria().andRoleEqualTo("approver").andNamaLikeInsensitive(newStr);
        ex.or(ex.createCriteria().andRoleEqualTo("approver").andJabatanLikeInsensitive(newStr));
        ex.or(ex.createCriteria().andRoleEqualTo("signer").andNamaLikeInsensitive(newStr));
        ex.or(ex.createCriteria().andRoleEqualTo("signer").andJabatanLikeInsensitive(newStr));
        List<User> listKode = userMapper.selectByExample(ex);
        HashMap<String, String> map = new HashMap<>();

        for (User kk : listKode) {
            if(kk.getId() > 0) {
                map.put(String.valueOf(kk.getId()), kk.getJabatan() + " - " + kk.getNama());
            }
        }
        return map;
    }

    @Transactional(readOnly = false)
    public void updateStatusSuratAfterUpload(Integer idSurat) {
        Surat suratObj = getSuratById(idSurat);
        suratObj.setStatusId(5);
        suratMapper.updateByPrimaryKey(suratObj);
    }

    @Transactional(readOnly = false)
    public void updateNomorSurat(Integer idSurat, String noSurat) throws ParseException {
        Surat suratObj = getSuratById(idSurat);
        suratObj.setNoSurat(noSurat);
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        Date date = formatter.parse(df.format(System.currentTimeMillis()));
        suratObj.setTanggalPembuatan(date);
        suratMapper.updateByPrimaryKey(suratObj);

    }

    public static boolean isNumeric(String str) {
        return str != null && str.matches("[-+]?\\d*\\.?\\d+");
    }

    public void generateTempFile(Surat surat) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException, DocumentException {
        Context context = new Context();

        if(isNumeric(surat.getSifat())) {
            SifatDetail sifatDetail = this.getJSONSifatById(surat.getSifat());
            surat.setSifat(this.convert(sifatDetail.getNama()));
        }
        if(surat.getTipe() == 1) {
            surat.setKepada(surat.getKepada().trim());
        }
        context.setVariable("surat", surat);
        context.setVariable("headerName", headerName(surat));

        String[] monthNames = {
                "Januari", "Februari", "Maret",
                "April", "Mei", "Juni", "Juli",
                "Agustus", "September", "Oktober",
                "November", "Desember"};
        if(surat.getTanggalPembuatan() != null) {
            context.setVariable("tanggal", surat.getTanggalPembuatan().getDate() + " " + monthNames[surat.getTanggalPembuatan().getMonth()] + " " + (1900 + surat.getTanggalPembuatan().getYear()));
        } else {
            context.setVariable("tanggal", "-");
        }


        context.setVariable("cetak", true);
        User pejabat = new User();
        pejabat.setNama("-");
        pejabat.setNip("-");
        pejabat.setJabatan("-");

        List<SuratSignDTO> signers = new ArrayList<SuratSignDTO>();
        if(surat.getStatusId() == -1) {
            signers = suratApproveService.getListSuratSignAndUser(surat.getId(), null);
        } else {
            signers = suratSignService.getListSuratSignDTO(surat.getId());
        }

        if(!signers.isEmpty()) {
            pejabat = userMapper.selectByPrimaryKey(signers.get(0).getIdUser());
        }
        context.setVariable("namaSigner", pejabat.getNama());
        context.setVariable("jabatanSigner", pejabat.getJabatan());
        context.setVariable("nip", pejabat.getNip());
        if(surat.getTipe() == 2){
            List<User> selectedKepada = new ArrayList<>();
            if(surat.getKepada()!=null){
                if(surat.getTipe() != null && surat.getTipe() == 2){
                    String[] parts = getSuratById(surat.getId()).getKepada().split(",");
                    for(String x: parts){
                        selectedKepada.add(userMapper.selectByPrimaryKey(Integer.parseInt(x)));
                    }
                }
            }
            context.setVariable("kepada",selectedKepada);
        }

        List<User> selectedTembusan = new ArrayList<>();
        Surat suratTembusan = getSuratById(surat.getId());
        if(suratTembusan.getTembusan()!=null && !suratTembusan.getTembusan().equals("")){
            String[] parts = suratTembusan.getTembusan().split(",");
            for(String x: parts){
                selectedTembusan.add(userMapper.selectByPrimaryKey(Integer.parseInt(x)));
            }
            System.out.println("selectedKepada:"+selectedTembusan.get(0).getJabatan());
            context.setVariable("tembusan", selectedTembusan);
        }

        String renderedHtmlContent = null;
        if (surat.getTipe() == 2) {
            renderedHtmlContent = templateEngine.process("nota-surat", context);
        } else {
            renderedHtmlContent = templateEngine.process("surat-dinas", context);
        }


        try {
            OutputStream file = new FileOutputStream(new File(folderPrefix+"/arsipui/temp/temp_" + surat.getId() + ".pdf"));
            Document document = new Document();
            PdfWriter.getInstance(document, file);
            document.open();
            HTMLWorker htmlWorker = new HTMLWorker(document);
            htmlWorker.parse(new StringReader(renderedHtmlContent));
            document.close();
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("html " + renderedHtmlContent);
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(renderedHtmlContent);
        renderer.layout();

        // And finally, we create the PDF:
        OutputStream outputStream = new FileOutputStream(folderPrefix+"/arsipui/temp/temp_" + surat.getId() + ".pdf");
        renderer.createPDF(outputStream);
        outputStream.flush();
        outputStream.close();

        if (surat.getFile() != null && !surat.getFile().isEmpty()) {
            PDFMergerUtility ut = new PDFMergerUtility();
            ut.addSource(folderPrefix+"/arsipui/temp/temp_" + surat.getId() + ".pdf");

            ut.addSource(folderUploadSurat+"/" + surat.getPembuat() + "/" + surat.getId() + "_" + surat.getFile());
            ut.setDestinationFileName(folderPrefix+"/arsipui/temp/temp_" + surat.getId() + ".pdf");
            ut.mergeDocuments();
        }
    }

    @Transactional(readOnly = false)
    public void kirimBsre(Surat surat, SignerVerifyDTO signer) throws Exception {

        generateTempFile(surat);

        List<SuratSignDTO> signers = suratSignService.getListSuratSignDTO(surat.getId());
        User pejabat = userMapper.selectByPrimaryKey(signers.get(0).getIdUser());

        Path path = Paths.get(folderPrefix+"/arsipui/temp/temp_" + surat.getId() + ".pdf");
        byte[] content = Files.readAllBytes(path);
//        String updatePersonUrl = "http://152.118.24.72:8080/api/sign/pdf?nik=" + signer.getNik().trim() + "&passphrase="
//                + URLEncoder.encode(signer.getPassphrase())
//                + "&tampilan=invisible";

        UriComponentsBuilder  uriBuilder = UriComponentsBuilder
                .fromUriString("http://152.118.24.241:80/api/sign/pdf")
                .queryParam("nik", signer.getNik().trim())
                .queryParam("passphrase", signer.getPassphrase())
                .queryParam("tampilan", "invisible");
//        StringBuilder builder = new StringBuilder("http://152.118.24.72:8080/api/sign/pdf");
//        builder.append("?nik=");
//        builder.append(URLEncoder.encode(signer.getNik().trim(), StandardCharsets.UTF_8.toString()));
//        builder.append("&passphrase=");
//        builder.append(URLEncoder.encode(signer.getPassphrase(), StandardCharsets.UTF_8.toString()));
////        builder.append(signer.getPassphrase());
//        builder.append("&tampilan=");
//        builder.append(URLEncoder.encode("invisible", StandardCharsets.UTF_8.toString()));

//        URI uri = URI.create(builder.toString());
//        log.info("passphrase: " + signer.getPassphrase());
//        log.info("uri:" + uri.toString());
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Authorization", "Basic Y2lwdGFuYXNrYWg6SmFpNm9vcGg=");
        MultiValueMap<String, Object> body
                = new LinkedMultiValueMap<>();
        body.add("Content-Type", MediaType.APPLICATION_PDF);

        ByteArrayResource bar = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "Test-" + content.length + ".pdf";
            }
        };

        body.add("file", bar);

        HttpEntity<MultiValueMap<String, Object>> requestEntity
                = new HttpEntity<>(body, headers);

        System.out.println(requestEntity.toString());
        try {
            log.info("coba ttd username " + pejabat.getUsername() + " nama " + pejabat.getNama());
            ResponseEntity<Resource> response = restTemplate.postForEntity(uriBuilder.build().toUri(), requestEntity, Resource.class);
            File fileDestination = new File(folderPrefix + "/arsipui/verified", "verified_" + surat.getId() + ".pdf");
            BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(fileDestination));
            FileCopyUtils.copy(response.getBody().getInputStream(), outStream);

            outStream.close();
        } catch (HttpClientErrorException e) {
            log.info(e.getResponseBodyAsString());

            // for local development, comment throw exception and the uncomment following code
//            if(!signer.getPassphrase().equals("passphrase")){
//                throw new Exception("Error BSRE: "+ e.getResponseBodyAsString());
//            }
//            File fileDestination = new File(folderPrefix + "/arsipui/verified", "verified_" + surat.getId() + ".pdf");
//            BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(fileDestination));
//            FileCopyUtils.copy(content, outStream);

            throw new Exception("Error BSRE: "+ e.getResponseBodyAsString());
        } catch (Exception e) {
            log.info("error ttd surat "+ pejabat.getUsername());
            e.printStackTrace();
            throw new Exception("Non-HTTP-Client BSRE Error");
        }

        //kirim ke api pak maman
        if(surat.getTipe().toString().equals("2") || (surat.getTembusan() != null && !surat.getTembusan().equals(""))) {
            path = Paths.get(folderPrefix+"/arsipui/verified/verified_" + surat.getId() + ".pdf");
            byte[] content2 = Files.readAllBytes(path);

            String updatePersonUrl2 = urlService + "/rest/insert_surat_masuk/";
            RestTemplate restTemplate2 = new RestTemplate();
            restTemplate2.setRequestFactory(getRequestFactory());

            HttpHeaders headers2 = new HttpHeaders();
            //headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers2.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers2.add("Authorization", "Basic Y2lwdGFuYXNrYWg6cHVzaWxrb20yMDIw");

            MultiValueMap<String, Object> body2
                    = new LinkedMultiValueMap<>();
            //body2.add("Content-Type", MediaType.APPLICATION_PDF);

            ByteArrayResource bar2 = new ByteArrayResource(content2) {
                @Override
                public String getFilename() {
                    return "verified_" + surat.getId() + ".pdf";
                }
            };

            User pembuat = userMapper.selectByPrimaryKey(surat.getPembuat());
            Integer unitKerja = null;
            Map<String, String> unitKerjaDetailList = getJSONUnitKerja("");
            for (Map.Entry<String,String> entry : unitKerjaDetailList.entrySet()) {
                if(entry.getValue().toUpperCase().equals(pembuat.getInitial().toUpperCase())){
                    unitKerja = Integer.parseInt(entry.getKey());
                }
            }
            List<Integer> arr = new ArrayList<Integer>();

            if(surat.getTipe() == 2) {
                if(getSuratById(surat.getId()).getKepada().contains(",")) {
                    String[] arrayString = getSuratById(surat.getId()).getKepada().split(",");
                    for (String string : arrayString) {
                        arr.add(Integer.parseInt(string));
                    }
                } else {
                    arr.add(Integer.parseInt(getSuratById(surat.getId()).getKepada()));
                }
            }

            arr.add(surat.getPembuat());

            if(!StringUtils.isBlank(getSuratById(surat.getId()).getTembusan())) {
                if(getSuratById(surat.getId()).getTembusan().contains(",")) {
                    String[] arrayString = getSuratById(surat.getId()).getTembusan().split(",");
                    for (String string : arrayString) {
                        arr.add(Integer.parseInt(string));
                    }
                } else {
                    arr.add(Integer.parseInt(getSuratById(surat.getId()).getTembusan()));
                }
            }
            System.out.println("unit kerja "+ unitKerja+ " "+pembuat.getInitial());
            if(unitKerja!=null) body2.add("unit_kerja", unitKerja.intValue());

            Integer derajat = 3;
            if(surat.getDerajat().equals("Biasa")) {
                derajat = 1;
            } else if (surat.getDerajat().equals("Segera")) {
                derajat = 2;
            }
            body2.add("sifat_surat", derajat.intValue());

            body2.add("tipe_surat", "I");

            body2.add("asal_surat", pembuat.getJabatan());
            body2.add("nomor_surat", surat.getNoSurat());
            body2.add("tanggal_surat", (1900 + surat.getTanggalPembuatan().getYear()) + "-" + (surat.getTanggalPembuatan().getMonth() + 1) + "-" + surat.getTanggalPembuatan().getDate());
            body2.add("perihal", surat.getPerihal());
            for(Integer int1 : arr) {
                body2.add("tujuan_distribusi", int1);
            }
            body2.add("klasifikasi_kegiatan", surat.getIdKodeKlasifikasi());
            body2.add("isi_singkat", "-");
            body2.add("referensi", "");
            body2.add("petugas_entri", pembuat.getUsername());
            body2.add("lampiran", bar2);
            body2.add("model", "surat_masuk");


            HttpEntity<MultiValueMap<String, Object>> requestEntity2
                    = new HttpEntity<>(body2, headers2);
            System.out.println(">>> header API Pak Maman : {}"+ requestEntity2.getHeaders());
            System.out.println(">>> body API Pak Maman : {}"+ requestEntity2.toString());

            ResponseEntity<ApiResult> res = null;
            try {
                res = restTemplate2.
                        exchange(updatePersonUrl2, HttpMethod.POST, requestEntity2, ApiResult.class);
            } catch (HttpClientErrorException e) {
                log.info(e.getResponseBodyAsString());
                throw new Exception("Error Andieni: " + e.getResponseBodyAsString());
            } catch (HttpServerErrorException e) {
                throw new Exception("Error Andieni: " + e.getResponseBodyAsString());
            } catch (Exception e) {
                log.info("error kirim surat "+ pejabat.getUsername());
                e.printStackTrace();
                throw new Exception("Error Andieni: " + e.getMessage());

            }

        }
    }

    private String getHtmlString(Map<String, Object> variables, String templatePath) {
        try {
            final Context ctx = new Context();
            ctx.setVariables(variables);
            return templateEngine.process(templatePath, ctx);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = false)
    public void updateStatusSuratEdit (Integer idSurat) {
        Surat surat = suratMapper.getSuratById(idSurat);
        if(surat != null){
            surat.setStatusId(-1);
            surat.setJumlahApproved(0);
            suratMapper.updateByPrimaryKeySelective(surat);
            resetApproverToDraft(idSurat);
            resetSignerToDraft(idSurat);
        }
    }

    @Transactional(readOnly = false)
    public void resetApproverToDraft(Integer idSurat) {
        Surat surat = this.getSuratById(idSurat);
        List<SuratApprove> listSuratApprove = suratApproveService.getListSuratApproveByCertainCases(idSurat, true);
        if (listSuratApprove != null && listSuratApprove.size() > 0 && surat.getStatusId() == -1) {
            for (SuratApprove approver : listSuratApprove) {
                approver.setFlag(false);
                suratApproveMapper.updateByPrimaryKey(approver);
            }

            for (SuratApprove approver : listSuratApprove) {
                SuratApprove suratApprove = new SuratApprove();
                suratApprove.setIdSurat(approver.getIdSurat());
                suratApprove.setIdUser(approver.getIdUser());
                suratApproveMapper.insert(suratApprove);
            }
        }

    }

    public void resetSignerToDraft(Integer idSurat) {
        List<SuratSign> listSuratSigner = suratSignService.getListSuratSignByCertainCases(idSurat, true);
        Surat surat = getSuratById(idSurat);
        if (listSuratSigner != null && listSuratSigner.size() > 0  && surat.getStatusId() == -1) {
            for (SuratSign signer : listSuratSigner) {
                signer.setFlag(false);
                suratSignMapper.updateByPrimaryKey(signer);
            }

            for (SuratSign signer : listSuratSigner) {
                SuratSign suratSign = new SuratSign();
                suratSign.setIdSurat(signer.getIdSurat());
                suratSign.setIdUser(signer.getIdUser());
                suratSignMapper.insert(suratSign);
            }
        }
    }

    @Transactional(readOnly = false)
    public Integer save(Surat surat) {
        if (surat.getId() == null) {
            suratMapper.insertSelective(surat);
        } else {
            suratMapper.updateByPrimaryKeySelective(surat);
        }
        System.out.println("surat inserted "+surat.getId());
        return surat.getId();
    }

    @Transactional(readOnly = false)
    public String getCounterNosurat(String noSurat) {
        String[] parts = noSurat.split("/");
        String[] parts2 = parts[0].split("-");
        String counter = parts2[1];
        return counter;
    }

    @Transactional(readOnly = false)
    public String validateAccessBuatSurat(Integer idSurat) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User detailUserLogin = userService.getUserDetailByUsername(authentication.getName());
        Surat suratById = this.getSuratById(idSurat);
        String notifikasi = "";

        SuratApprove approve = suratApproveService.getApproveByIdSuratIdUser(idSurat, detailUserLogin.getId());
        SuratSign sign = suratSignService.getSuratSignByIdSuratIdUser(idSurat, detailUserLogin.getId());

        if(suratById==null){
            notifikasi = "Naskah dinas tidak terdaftar";
        }  else if(suratById.getStatusId() == -1){
            if(detailUserLogin.getId().toString().equals(suratById.getPembuat().toString())){
            }else{
                notifikasi = "Anda bukan pembuat surat ini";
            }
        } else if(suratById.getStatusId() == 0){
            if(approve.getId() != null){
                if(approve.getStatus() == null){

                }else{
                    notifikasi = "Surat tidak dapat diubah, Anda sudah melakukan proses approval";
                }
            }else {
                notifikasi = "Anda bukan approver surat ini";
            }
        }else if(suratById.getStatusId() == 2){
            if(sign.getId() != null){
            }else{
                notifikasi = "Anda bukan signer surat ini";
            }
        }else if(suratById.getStatusId() == 1 || suratById.getStatusId() == 3){
            if(detailUserLogin.getId().toString().equals(suratById.getPembuat().toString())){
                notifikasi = "Ubah surat dapat dilakukan melalui menu Edit Naskah";
            }else{
                notifikasi = "Anda bukan pembuat surat ini";
            }
        }else if(suratById.getStatusId() == 4 || suratById.getStatusId() == 5){
            if(detailUserLogin.getId().toString().equals(suratById.getPembuat().toString()) || approve.getId() != null || sign.getId() != null){
                notifikasi = "Surat tidak dapat diubah lagi";
            }else{
                notifikasi = "Anda tidak berhak membuka surat ini";
            }
        }


        /*else if (!detailUserLogin.getId().toString().equals(suratById.getPembuat().toString()) || approve.getId() == null || sign.getId() == null) {
            notifikasi = "Anda bukan pembuat/approver/signer surat ini";
        }*/
        /*else if(approve.getId() != null && suratById.getStatusId() != 0){
            notifikasi = "Surat tidak bisa diedit karena tidak dalam proses approval";
        } else if(approve.getId()  != null && suratById.getStatusId() == 0 && approve.getStatus() != null){
            notifikasi = "Anda sudah melakukan proses approval pada surat ini";
        } else if(sign.getId()  != null && suratById.getStatusId() != 2){
            notifikasi = "Surat tidak bisa diedit karena tidak dalam proses penandatanganan";
        }*/
        /*else if(suratById.getStatusId()!=-1 && detailUserLogin.getId().toString().equals(suratById.getPembuat().toString())){
            notifikasi = "Naskah dinas sedang dalam persetujuan/penandatanganan";
        } */
        /*} else if(approve.getId() != null && suratById.getStatusId() > 0 && sign.getId() == null){
            notifikasi = "Surat tidak bisa diedit karena proses approval sudah selesai";
        } else if(sign.getId()  != null && suratById.getStatusId() == 2){
            notifikasi = "Surat tidak bisa diedit";
        }*/


        return notifikasi;
    }

    @Transactional(readOnly = false)
    public void hapusLampiran(Integer idSurat) {
        Surat surat = this.getSuratById(idSurat);
        File oldFile = new File (folderUploadSurat+"/"+surat.getPembuat()+"/"+surat.getId()+"_"+surat.getFile());
        oldFile.delete();
        surat.setFile(null);
        suratMapper.updateByPrimaryKey(surat);
    }

    @Transactional(readOnly = false)
    public Map<String, String> getJSONUser(String jabatan) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String uri = urlService+"/rest/pengguna/";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(getRequestFactory());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic Y2lwdGFuYXNrYWg6cHVzaWxrb20yMDIw");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        //log.info(">>> getresult : {}", d.toString(res.getBody()));

        ObjectMapper mapper = new ObjectMapper();
        List<PenggunaDetail> participantJsonList = mapper.readValue(res.getBody(), new TypeReference<List<PenggunaDetail>>(){});

        HashMap<String, String> mapList = new HashMap<>();
        for (PenggunaDetail obj : participantJsonList) {
            if(obj.getJabatan().toLowerCase().contains(jabatan.toLowerCase())){
                mapList.put(String.valueOf(obj.getId()), obj.getJabatan() + " - " + obj.getNama());
            }
        }

        return mapList;
    }

    @Transactional(readOnly = false)
    public boolean migrateUser() throws IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        if(dump.equals("yes")) {
            dumpUser();
        }

        UserExample ex3 = new UserExample();
        ex3.createCriteria().andCounterNotaGreaterThan(0);
        Integer hasNotaBefore = userMapper.countByExample(ex3);

        UserExample ex4 = new UserExample();
        ex4.createCriteria().andCounterSuratGreaterThan(0);
        Integer hasSuratBefore =  userMapper.countByExample(ex4);

        // necessary config for API call
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(getRequestFactory());
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic Y2lwdGFuYXNrYWg6cHVzaWxrb20yMDIw");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
        ObjectMapper mapper = new ObjectMapper();

        // get pengguna
        String uri = urlService+"/rest/pengguna/";
        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        //log.info(">>> getresult : {}", d.toString(res.getBody()));
        List<PenggunaDetail> participantJsonList = mapper.readValue(res.getBody(), new TypeReference<List<PenggunaDetail>>(){});

        // get users
        String uriUsers = urlService+"/rest/users/";
        res = restTemplate.exchange(uriUsers, HttpMethod.GET, entity, String.class);
        List<UserDetail> users = mapper.readValue(res.getBody(), new TypeReference<List<UserDetail>>(){});

        // get esign_data
        String uriEsign = urlService+"/rest/esign_data/";
        res = restTemplate.exchange(uriEsign, HttpMethod.GET, entity, String.class);
        List<EsignData> listEsignData = mapper.readValue(res.getBody(), new TypeReference<List<EsignData>>(){});

        String uriUnitKerja = urlService+"/rest/konfigurasi_unit_kerja/";
        res = restTemplate.exchange(uriUnitKerja, HttpMethod.GET, entity, String.class);
        List<UnitKerjaDetail> unitKerjas = mapper.readValue(res.getBody(), new TypeReference<List<UnitKerjaDetail>>(){});

        for(PenggunaDetail detail : participantJsonList) {
            User user = new User();

            user.setUnit("-");

            if (detail.getUnit_kerja() == null) {
                user.setUnit("-");
            } else if (detail.getUnit_kerja().equalsIgnoreCase("pusilkom")) {
                user.setUnit("PUSILKOM");
            } else {
                for (UnitKerjaDetail unitKerjaDetail : unitKerjas) {
                    if (unitKerjaDetail.getNama().equals(detail.getUnit_kerja())) {
                        user.setUnit(unitKerjaDetail.getDesc());
                    }
                }
            }


            // set nik
            Boolean hasNik = false;
            for (UserDetail userDetail : users) {
                if (detail.getUser().equals(userDetail.getUsername())) {
                    for (EsignData esignData : listEsignData) {
                        if (userDetail.getUrl().equals(esignData.getUser())) {
                            user.setNik(esignData.getNik());
                            hasNik = true;
                        }
                    }
                }
            }
            if (!hasNik) {
                user.setNik("-");
            }

            user.setId(detail.getId());

            user.setNama(detail.getNama());
            user.setEmail(detail.getEmail());
            if (detail.getKode_identitas() != null && !detail.getKode_identitas().equals("")) {
                user.setNip(detail.getKode_identitas());
            } else {
                user.setNip("-");
            }

            user.setRole("signer");

            if(detail.getJabatan() == null) {
                user.setJabatan("-");
            } else if(!detail.getJabatan().trim().equalsIgnoreCase(detail.getUnit_kerja().trim()) && !detail.getJabatan().equals("NONE")) {
                if(detail.getJabatan().toLowerCase().trim().contains(detail.getUnit_kerja().toLowerCase().trim())
                        || detail.getUnit_kerja().toLowerCase().trim().contains(detail.getJabatan().toLowerCase().trim())) {
                    user.setJabatan(detail.getJabatan());
                } else if(detail.getJabatan().toLowerCase().contains("direktur") && detail.getUnit_kerja().toLowerCase().contains("direktorat")) {
                    user.setJabatan(detail.getJabatan());
                } else {
                    user.setJabatan(detail.getJabatan().trim() + " " + detail.getUnit_kerja().trim());
                }
            } else {
                user.setJabatan(detail.getUnit_kerja().trim());
            }
            if(detail.getUnit_kerja() == null) {
                user.setInitial("-");
            } else {
                user.setInitial(detail.getUnit_kerja().toLowerCase());
            }
            user.setUsername(detail.getUser().trim());

            user.setCounterNota(0);
            user.setCounterSurat(0);
            userMapper.upsertUser(user);
        }

        UserExample ex = new UserExample();
        ex.createCriteria().andCounterNotaGreaterThan(0);
        Integer hasNotaAfter = userMapper.countByExample(ex);

        UserExample ex2 = new UserExample();
        ex2.createCriteria().andCounterSuratGreaterThan(0);
        Integer hasSuratAfter = userMapper.countByExample(ex2);


        return (hasNotaAfter.intValue() >= hasNotaBefore.intValue()) && (hasSuratAfter.intValue() >= hasSuratBefore.intValue()) &&
                (userMapper.countUsers().intValue() == userMapper.countDistinctUsers().intValue());

    }

    public String convert(String str)
    {

        // Create a char array of given String
        char ch[] = str.toCharArray();
        for (int i = 0; i < str.length(); i++) {

            // If first character of a word is found
            if (i == 0 && ch[i] != ' ' ||
                    ch[i] != ' ' && ch[i - 1] == ' ') {

                // If it is in lower-case
                if (ch[i] >= 'a' && ch[i] <= 'z') {

                    // Convert into Upper-case
                    ch[i] = (char)(ch[i] - 'a' + 'A');
                }
            }

            // If apart from first character
            // Any one is in Upper-case
            else if (ch[i] >= 'A' && ch[i] <= 'Z')

                // Convert into Lower-Case
                ch[i] = (char)(ch[i] + 'a' - 'A');
        }

        // Convert the char array to equivalent String
        String st = new String(ch);
        return st;
    }

    @Transactional(readOnly = false)
    public PenggunaDetail getJSONUserById(Integer id) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        System.out.println(urlService+"/rest/pengguna/" + id);
        String uri = urlService+"/rest/pengguna/" + id;
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(getRequestFactory());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic Y2lwdGFuYXNrYWg6cHVzaWxrb20yMDIw");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        //log.info(">>> getresult : {}", d.toString(res.getBody()));

        ObjectMapper mapper = new ObjectMapper();
        PenggunaDetail participantJsonList = mapper.readValue(res.getBody(), new TypeReference<PenggunaDetail>(){});

        return participantJsonList;
    }

    @Transactional(readOnly = false)
    public Boolean checkStringIsInteger(String string) {
        Boolean isInteger;
        int num = -1;
        if(string!=null){
            try{
                num = Integer.parseInt(string);
                isInteger = true;
            } catch (NumberFormatException e) {
                isInteger = false;
            }
        } else {
            isInteger = false;
        }
        return isInteger;
    }

    @Transactional(readOnly = false)
    public Map<String, String> getDropdownUserByUsername(String searchName) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User detailUserLogin = userService.getUserDetailByUsername(authentication.getName());

        String uri = urlService+"/rest/dropdown_for_surat_masuk/?username=" + detailUserLogin.getUsername();
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(getRequestFactory());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic Y2lwdGFuYXNrYWg6cHVzaWxrb20yMDIw");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, String> mapList = new HashMap<>();
        try{
            ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            //log.info(">>> getresult : {}", d.toString(res.getBody()));
            DropdownKepadaDetail participantJsonList = mapper.readValue(res.getBody(), new TypeReference<DropdownKepadaDetail>(){});
            //log.info(">>> getresult 2: {}", d.toString(participantJsonList));

            for (DropdownKepadaDetailChild3 obj : participantJsonList.getFields().getTujuan_distribusi().getData()) {
                User user = userMapper.selectByPrimaryKey(obj.getId());
                if(user != null) {
                    String searched = user.getJabatan() + " - " + obj.getNama();
                    if (searchName == null || searchName.equals("")) {
                        mapList.put(String.valueOf(obj.getId()), searched);
                    } else {
                        if (searched.toLowerCase().contains(searchName.toLowerCase())) {
                            mapList.put(String.valueOf(obj.getId()), searched);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mapList;
    }

    @Transactional(readOnly = false)
    public Map<String, String> getJSONUnitKerja(String searchName) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String uri = urlService+"/rest/konfigurasi_unit_kerja/";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(getRequestFactory());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic Y2lwdGFuYXNrYWg6cHVzaWxrb20yMDIw");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        //log.info(">>> getresult : {}", d.toString(res.getBody()));

        ObjectMapper mapper = new ObjectMapper();
        List<UnitKerjaDetail> participantJsonList = mapper.readValue(res.getBody(), new TypeReference<List<UnitKerjaDetail>>(){});

        HashMap<String, String> mapList = new HashMap<>();
        for (UnitKerjaDetail obj : participantJsonList) {
            if(obj.getNama().toLowerCase().contains(searchName.toLowerCase())){
                mapList.put(String.valueOf(obj.getId()), obj.getNama());
            }
        }

        return mapList;
    }

    @Transactional(readOnly = false)
    public UnitKerjaDetail getJSONUnitKerjaById(Integer idUnitKerja) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String uri = urlService+"/rest/konfigurasi_unit_kerja/" + idUnitKerja;
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(getRequestFactory());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic Y2lwdGFuYXNrYWg6cHVzaWxrb20yMDIw");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        //log.info(">>> getresult unit kerja: {}", d.toString(res.getBody()));

        ObjectMapper mapper = new ObjectMapper();
        UnitKerjaDetail participantJsonList = mapper.readValue(res.getBody(), new TypeReference<UnitKerjaDetail>(){});


        return participantJsonList;
    }

    @Transactional(readOnly = false)
    public Map<String, String> getJSONSifat(String sifat) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String uri = urlService+"/rest/sifat_surat/";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(getRequestFactory());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic Y2lwdGFuYXNrYWg6cHVzaWxrb20yMDIw");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        //log.info(">>> getresult : {}", d.toString(res.getBody()));

        ObjectMapper mapper = new ObjectMapper();
        List<SifatDetail> participantJsonList = mapper.readValue(res.getBody(), new TypeReference<List<SifatDetail>>(){});

        HashMap<String, String> mapList = new HashMap<>();
        if(sifat != null && !sifat.isEmpty()){
            for (SifatDetail obj : participantJsonList) {
                if(obj.getNama().toLowerCase().contains(sifat.toLowerCase())){
                    mapList.put(String.valueOf(obj.getId()), obj.getNama());
                }
            }
        } else {
            for (SifatDetail obj : participantJsonList) {
                mapList.put(String.valueOf(obj.getId()), obj.getNama());
            }
        }
        return mapList;
    }

    @Transactional(readOnly = false)
    public SifatDetail getJSONSifatById(String idSifat) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String uri = urlService+"/rest/sifat_surat/" + idSifat;
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(getRequestFactory());
        System.out.println("URI SIFAT" + uri);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic Y2lwdGFuYXNrYWg6cHVzaWxrb20yMDIw");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        //log.info(">>> getresult sifat: {}", d.toString(res.getBody()));

        ObjectMapper mapper = new ObjectMapper();
        SifatDetail participantJsonList = mapper.readValue(res.getBody(), new TypeReference<SifatDetail>(){});

        return participantJsonList;
    }

    @Transactional(readOnly = false)
    public String convertListKepadaToString(String kepada) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String listNama = "";
        if(kepada != null && !kepada.equals("")) {
            String[] parts = kepada.split(",");
            for (String x : parts) {
                PenggunaDetail kepadaDetail = this.getJSONUserById(Integer.parseInt(x));
                if (listNama.equals("")) {
                    listNama = listNama + kepadaDetail.getNama();
                } else {
                    listNama = listNama + ", " + kepadaDetail.getNama();
                }
            }
        }
        return listNama;
    }

    public HttpComponentsClientHttpRequestFactory getRequestFactory() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

        final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault());
        final HttpClient httpClient = HttpClientBuilder.create()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setSSLSocketFactory(csf)
                .build();
        factory.setHttpClient(httpClient);

        return factory;
    }

    public void dumpUser() throws IOException {
        ProcessBuilder pb;
        pb = new ProcessBuilder("/usr/bin/pg_dump", "-h", "localhost", "--table=t_user", "--data-only", "--column-inserts", "ciptanaskah > /home/ciptanaskah/t_user_baru.sql", "-U", "ciptanaskah");
        pb.environment().put("PGPASSWORD","eo1Vagheiphe");
        pb.redirectErrorStream(true);
        Process pr = pb.start();

        try (InputStreamReader inputStreamReader = new InputStreamReader(pr.getInputStream());
             BufferedReader input = new BufferedReader(inputStreamReader)
        ) {
            String line = null;
            while ((line = input.readLine()) != null) {

            }
            int exitVal = pr.waitFor();
            log.info("Exited with error code {}", exitVal);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }
}
