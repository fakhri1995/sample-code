package com.pusilkom.arsipui.service;

import com.pusilkom.arsipui.dto.table.SignerVerifyDTO;
import com.pusilkom.arsipui.dto.table.SuratSignDTO;
import com.pusilkom.arsipui.model.Surat;
import com.pusilkom.arsipui.model.SuratSign;
import com.pusilkom.arsipui.model.SuratSignExample;
import com.pusilkom.arsipui.model.User;
import com.pusilkom.arsipui.model.mapper.SuratMapper;
import com.pusilkom.arsipui.model.mapper.SuratSignMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SuratSignService {
    @Autowired
    SuratSignMapper suratSignMapper;
    @Autowired
    SuratService suratService;
    @Autowired
    SuratMapper suratMapper;
    @Autowired
    UserService userService;
    Logger log = LoggerFactory.getLogger(this.getClass());

    @Transactional(readOnly = false)
    public List<SuratSign> getListSuratSignByIdSurat(Integer id) {
        List<SuratSign> listSign;
        SuratSignExample ex = new SuratSignExample();
        ex.createCriteria().andIdSuratEqualTo(id).andFlagEqualTo(true);
        listSign = suratSignMapper.selectByExample(ex);
        return listSign;
    }

    @Transactional(readOnly = false)
    public List<SuratSign> getListSuratSignByIdSuratAllFlag(Integer id) {
        List<SuratSign> listSign;
        SuratSignExample ex = new SuratSignExample();
        ex.createCriteria().andIdSuratEqualTo(id);
        listSign = suratSignMapper.selectByExample(ex);
        return listSign;
    }

    @Transactional(readOnly = false)
    public SuratSign getSuratSignByIdSuratIdUser (Integer idSurat, Integer idUser) {
        SuratSign sign = new SuratSign();
        SuratSignExample ex = new SuratSignExample();
        ex.createCriteria().andIdSuratEqualTo(idSurat).andIdUserEqualTo(idUser).andFlagEqualTo(true);
        List<SuratSign> list = suratSignMapper.selectByExample(ex);
        if(list.size() > 0) {
            try {
                sign = suratSignMapper.selectByPrimaryKey(list.get(0).getId());
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return sign;
    }

    @Transactional(readOnly = false)
    public void saveTidakTandaTangan(SuratSign sign, Integer idUser) {
        SuratSign approve = new SuratSign();
        SuratSignExample ex = new SuratSignExample();
        ex.createCriteria().andIdSuratEqualTo(sign.getIdSurat()).andIdUserEqualTo(idUser).andFlagEqualTo(true);
        List<SuratSign> list = suratSignMapper.selectByExample(ex);
        if(list.size() > 0) {
            try {
                approve = suratSignMapper.selectByPrimaryKey(list.get(0).getId());
                approve.setStatus(false);
                approve.setTanggal(new Date());
                approve.setKomentar(sign.getKomentar());
                suratSignMapper.updateByPrimaryKey(approve);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

    }

    @Transactional(readOnly = false)
    public void saveTandaTangan(SuratSign sign, Integer idUser) {
        SuratSign approve = new SuratSign();
        SuratSignExample ex = new SuratSignExample();
        ex.createCriteria().andIdSuratEqualTo(sign.getIdSurat()).andIdUserEqualTo(idUser).andFlagEqualTo(true);
        List<SuratSign> list = suratSignMapper.selectByExample(ex);
        if(list.size() > 0) {
            try {
                approve = suratSignMapper.selectByPrimaryKey(list.get(0).getId());
                approve.setStatus(true);
                approve.setTanggal(new Date());
                approve.setKomentar(sign.getKomentar());
                suratSignMapper.updateByPrimaryKey(approve);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

    }
    @Transactional(readOnly = false)
    public void sign(Surat detailSurat, SuratSign sign, SignerVerifyDTO signer, User detailUserLogin) throws Exception {
        //set no surat
        String preview = suratService.previewNoSurat(detailSurat.getTipe(), detailUserLogin.getId(), detailSurat.getIdKodeKlasifikasi(), true, "");
        //log.info(">>>> detailSurat.getTipe() " + detailSurat.getTipe() + " sign.getIdUser() " + detailUserLogin.getId() + " detailSurat.getIdKodeKlasifikasi() " + detailSurat.getIdKodeKlasifikasi());
        //log.info(">>>> Preview => " + preview);
        //update no surat
        suratService.updateNomorSurat(sign.getIdSurat(), preview);
        detailSurat = suratService.getSuratById(sign.getIdSurat());
        //if credential err occur (400 bad req), catched below and show err msg on errorText
        suratService.kirimBsre(detailSurat, signer);
        //set tidak setuju di approve
        this.saveTandaTangan(sign, detailUserLogin.getId());
        //update jumlah setuju di t_surat
        suratService.updateJumlahSigned(sign.getIdSurat());
        // update counter nota/surat dinas
        userService.updateUserCounter(detailSurat.getId(), detailUserLogin.getId());
        //keluarin jumlah yang uda respon dr t_surat_approve -- sampai jumlah yang != null sama dengan jumlah approval
        //set status di t_surat
        this.updateStatusSuratAfterSigned(sign.getIdSurat());


    }
    @Transactional(readOnly = false)
    public void updateStatusSuratAfterSigned(Integer idSurat) {
        //keluarin jumlah yang uda respon dr t_surat_approve -- sampai jumlah yang != null sama dengan jumlah approval
        //set status di t_surat
        SuratSignExample ex = new SuratSignExample();
        ex.createCriteria().andIdSuratEqualTo(idSurat).andStatusIsNotNull().andFlagEqualTo(true);
        List<SuratSign> listSign = suratSignMapper.selectByExample(ex);
        Surat suratObj = suratService.getSuratById(idSurat);
        log.info(">>>> listSign size : " + listSign.size());
        log.info(">>>> jumlah signee : " + suratObj.getJumlahSignee());
        log.info(">>>> jumlah signed : " + suratObj.getJumlahSigned());
        if (listSign.size() == suratObj.getJumlahSignee()) {
            if(listSign.size() == suratObj.getJumlahSigned()){
                suratObj.setStatusId(5);
            }else{
                suratObj.setStatusId(3);
            }
        }
        suratMapper.updateByPrimaryKey(suratObj);
    }

    @Transactional(readOnly = false)
    public List<SuratSignDTO> getListSuratSignDTO(Integer idSurat) {
        List<SuratSignDTO> list = suratSignMapper.selectListSigner(idSurat);
        return list;
    }

    @Transactional(readOnly = false)
    public List<SuratSignDTO> getListSuratApproveDTO(Integer idSurat) {
        List<SuratSignDTO> list = suratSignMapper.selectListApprover(idSurat);
        return list;
    }

    @Transactional(readOnly = false)
    public List<SuratSignDTO> getListKomentarApprover(Integer idSurat) {
        List<SuratSignDTO> list = suratSignMapper.selectListKomentarApprover(idSurat);
        for(SuratSignDTO row : list){
            if(row.getTanggal() != null){
                DateFormat df = new SimpleDateFormat("dd-MM-YYYY hh:mm:ss aa");
                row.setTanggalString(df.format(row.getTanggal()));
            } else {
                row.setTanggalString("-");
            }
        }
        return list;
    }

    @Transactional(readOnly = false)
    public List<SuratSignDTO> getListKomentarSigner(Integer idSurat) {
        List<SuratSignDTO> list = suratSignMapper.selectListKomentarSigner(idSurat);
        for(SuratSignDTO row : list){
            if(row.getTanggal() != null){
                DateFormat df = new SimpleDateFormat("dd-MM-YYYY hh:mm:ss aa");
                row.setTanggalString(df.format(row.getTanggal()));
            } else {
                row.setTanggalString("-");
            }
        }
        return list;
    }

    @Transactional(readOnly = false)
    public List<SuratSign> getListSuratSignByCertainCases(Integer id, Boolean flag) {
        SuratSignExample ex = new SuratSignExample();
        if(flag==null){
            ex.createCriteria().andIdSuratEqualTo(id).andFlagIsNull();
        } else {
            ex.createCriteria().andIdSuratEqualTo(id).andFlagEqualTo(flag);
        }
        List<SuratSign> listSign = suratSignMapper.selectByExample(ex);
        return listSign;
    }

    @Transactional(readOnly = false)
    public List<SuratSignDTO> getKomentarApprover(Integer idSurat) {
        List<SuratSignDTO> list = suratSignMapper.selectKomentarApprover(idSurat);
        for(SuratSignDTO row : list){
            if(row.getStatus()!=null){
                if(row.getStatus()==true){
                    row.setStatusString("Setuju");
                } else if(row.getStatus()==false){
                    row.setStatusString("Tidak Setuju");
                }
            } else {
                row.setStatusString("-");
            }

            if(row.getKomentar().isEmpty()) row.setKomentar("-");
            if(row.getTanggal() != null){
                DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                row.setTanggalString(df.format(row.getTanggal()));
            } else {
                row.setTanggalString("-");
            }
        }
        return list;
    }

    @Transactional(readOnly = false)
    public List<SuratSignDTO> getKomentarSigner(Integer idSurat) {
        List<SuratSignDTO> list = suratSignMapper.selectKomentarSigner(idSurat);
        for(SuratSignDTO row : list){
            if(row.getStatus()!=null){
                if(row.getStatus()==true){
                    row.setStatusString("Setuju");
                } else if(row.getStatus()==false){
                    row.setStatusString("Tidak Setuju");
                }
            } else {
                row.setStatusString("-");
            }

            if(row.getKomentar().isEmpty()) row.setKomentar("-");
            if(row.getTanggal() != null){
                DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                row.setTanggalString(df.format(row.getTanggal()));
            } else {
                row.setTanggalString("-");
            }
        }
        return list;
    }
}
