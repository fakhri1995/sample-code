package com.pusilkom.arsipui.service;

import com.pusilkom.arsipui.dto.table.SuratSignDTO;
import com.pusilkom.arsipui.model.*;
import com.pusilkom.arsipui.model.mapper.SuratApproveMapper;
import com.pusilkom.arsipui.model.mapper.SuratMapper;
import com.pusilkom.arsipui.model.mapper.SuratSignMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SuratApproveService {
    @Autowired
    SuratApproveMapper suratApproveMapper;
    @Autowired
    SuratSignMapper suratSignMapper;
    @Autowired
    SuratMapper suratMapper;
    @Autowired
    SuratService suratService;
    Logger log = LoggerFactory.getLogger(this.getClass());

    public SuratApprove getApproveByIdSuratIdUser(Integer idSurat, Integer idUser){
        SuratApprove approve = new SuratApprove();
        SuratApproveExample ex = new SuratApproveExample();
        ex.createCriteria().andIdSuratEqualTo(idSurat).andIdUserEqualTo(idUser).andFlagEqualTo(true);
        List<SuratApprove> list = suratApproveMapper.selectByExample(ex);
        if(list.size() > 0) {
            try {
                approve = suratApproveMapper.selectByPrimaryKey(list.get(0).getId());
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return approve;
    }

    @Transactional(readOnly = false)
    public List<SuratApprove> getListSuratApproveByIdSurat(Integer id) {
        List<SuratApprove> listApprove;
        SuratApproveExample ex = new SuratApproveExample();
        ex.createCriteria().andIdSuratEqualTo(id).andFlagEqualTo(true);
        listApprove = suratApproveMapper.selectByExample(ex);
            return listApprove;
    }

    @Transactional(readOnly = false)
    public List<SuratApprove> getListSuratApproveByIdSuratAllFlag(Integer id) {
        List<SuratApprove> listApprove;
        SuratApproveExample ex = new SuratApproveExample();
        ex.createCriteria().andIdSuratEqualTo(id);
        listApprove = suratApproveMapper.selectByExample(ex);
        return listApprove;
    }

    @Transactional(readOnly = false)
    public void saveTidakSetuju(SuratApprove app, Integer idUser) {
        SuratApprove approve = new SuratApprove();
        SuratApproveExample ex = new SuratApproveExample();
        ex.createCriteria().andIdSuratEqualTo(app.getIdSurat()).andIdUserEqualTo(idUser).andFlagEqualTo(true);
        List<SuratApprove> list = suratApproveMapper.selectByExample(ex);
        if(list.size() > 0) {
            try {
                approve = suratApproveMapper.selectByPrimaryKey(list.get(0).getId());
                approve.setStatus(false);
                approve.setKomentar(app.getKomentar());

                java.util.Date utilDate = new java.util.Date();
                System.out.println("java.util.Date time    : " + utilDate);
                java.sql.Timestamp sqlTS = new java.sql.Timestamp(utilDate.getTime());
                System.out.println("java.sql.Timestamp time: " + sqlTS);
                DateFormat df = new SimpleDateFormat("dd/MM/YYYY hh:mm:ss:SSS");
                System.out.println("Date formatted         : " + df.format(utilDate));

                approve.setTanggal(sqlTS);
                suratApproveMapper.updateByPrimaryKey(approve);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

    }

    @Transactional(readOnly = false)
    public void saveSetuju(SuratApprove app, Integer idUser) {
        SuratApprove approve = new SuratApprove();
        SuratApproveExample ex = new SuratApproveExample();
        ex.createCriteria().andIdSuratEqualTo(app.getIdSurat()).andIdUserEqualTo(idUser).andFlagEqualTo(true);
        List<SuratApprove> list = suratApproveMapper.selectByExample(ex);
        if(list.size() > 0) {
            try {
                approve = suratApproveMapper.selectByPrimaryKey(list.get(0).getId());
                approve.setStatus(true);
                approve.setKomentar(app.getKomentar());

                java.util.Date utilDate = new java.util.Date();
                System.out.println("java.util.Date time    : " + utilDate);
                java.sql.Timestamp sqlTS = new java.sql.Timestamp(utilDate.getTime());
                System.out.println("java.sql.Timestamp time: " + sqlTS);
                DateFormat df = new SimpleDateFormat("dd/MM/YYYY hh:mm:ss");
                System.out.println("Date formatted         : " + df.format(utilDate));

                approve.setTanggal(utilDate);

                suratApproveMapper.updateByPrimaryKeySelective(approve);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

    }

    @Transactional(readOnly = false)
    public void updateStatusSuratAfterApproved(Integer idSurat) {
        //keluarin jumlah yang uda respon dr t_surat_approve -- sampai jumlah yang != null sama dengan jumlah approval
        //set status di t_surat
        SuratApproveExample ex = new SuratApproveExample();
        ex.createCriteria().andIdSuratEqualTo(idSurat).andStatusIsNotNull().andFlagEqualTo(true);
        List<SuratApprove> listApprove = suratApproveMapper.selectByExample(ex);
        Surat suratObj = suratService.getSuratById(idSurat);
        log.info(">>>> listApprove size : " + listApprove.size());
        log.info(">>>> jumlah approval : " + suratObj.getJumlahApproval());
        log.info(">>>> jumlah approved : " + suratObj.getJumlahApproved());
        if (listApprove.size() == suratObj.getJumlahApproval()) {
            if (listApprove.size() == suratObj.getJumlahApproved()) {
                suratObj.setStatusId(2);
                suratMapper.updateByPrimaryKeySelective(suratObj);
            }else if(listApprove.size() > suratObj.getJumlahApproved()){
                suratObj.setStatusId(1);
                suratMapper.updateByPrimaryKeySelective(suratObj);
            }
        }
    }

    @Transactional(readOnly = false)
    public List<SuratApprove> getListSuratApproveByCertainCases(Integer id, Boolean flag) {
        SuratApproveExample ex = new SuratApproveExample();
        if(flag==null){
            ex.createCriteria().andIdSuratEqualTo(id).andFlagIsNull();
        } else {
            ex.createCriteria().andIdSuratEqualTo(id).andFlagEqualTo(flag);
        }

        List<SuratApprove> listApprove = suratApproveMapper.selectByExample(ex);
        return listApprove;
    }

    @Transactional(readOnly = false)
    public List<SuratSignDTO> getListSuratApproveAndUser(Integer suratId, Boolean flag) {
        List<SuratSignDTO> listApprove = suratSignMapper.getListSuratApproveAndUser(suratId, flag);
        return listApprove;
    }

    @Transactional(readOnly = false)
    public List<SuratSignDTO> getListSuratSignAndUser(Integer suratId, Boolean flag) {
        List<SuratSignDTO> listSign = suratSignMapper.getListSuratSignAndUser(suratId, flag);
        return listSign;
    }
}
