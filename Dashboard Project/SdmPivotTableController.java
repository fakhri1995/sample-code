package com.unhan.dashboard.controller;

import com.unhan.dashboard.service.PenelitianService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;

@Controller
public class SdmPivotTableController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdmPivotTableController.class);
    private static final String TERMS_URI_FMT = "%s/api/terms";

    @Value("${euis.domain}")
    private String euisDomain;

    @Autowired
    PenelitianService penelitianService;

    @GetMapping("/distribusi-jumlah-karyawan-berdasarkan-tingkat-pendidikan")
    @Secured("ROLE_AUTHORIZED")
    public String distribusiJumlahKaryawanBerdasarkanTingkatPendidikan(Model uiModel) {
        uiModel.addAttribute("uriBackend", "\"/get-distribusi-jumlah-karyawan-berdasarkan-tingkat-pendidikan\"");
        uiModel.addAttribute("chartForm", "\"Bar Chart\"");
        uiModel.addAttribute("config", "'{\"rows\":[], " +
            "\"cols\":[\"pendidikan\"],"+
            "\"vals\":[\"total_employee\"],\"aggregatorName\":\"Sum\"}'");
        uiModel.addAttribute("menu", "SDM");
        uiModel.addAttribute("title", "Distribusi Jumlah Karyawan Berdasarkan Tingkat Pendidikan");

        return "pivot-table";
    }

    @GetMapping("/distribusi-jumlah-dosen-tetap-dan-tidak-tetap-berdasarkan-tingkat-pendidikan")
    @Secured("ROLE_AUTHORIZED")
    public String distribusiJumlahDosenTetapDanTidakTetapBerdasarkanTingkatPendidikan(Model uiModel) {
        uiModel.addAttribute("uriBackend", "\"/get-distribusi-jumlah-dosen-tetap-dan-tidak-tetap-berdasarkan-tingkat-pendidikan\"");
        uiModel.addAttribute("chartForm", "\"Bar Chart\"");
        uiModel.addAttribute("config", "'{\"rows\":[\"pendidikan\"], " +
            "\"cols\":[],"+
            "\"vals\":[\"total_employee\"],\"aggregatorName\":\"Sum\"}'");
        uiModel.addAttribute("menu", "SDM");
        uiModel.addAttribute("title", "Distribusi Jumlah Dosen tetap dan Tidak Tetap Berdasarkan Pendidikan");

        return "pivot-table";
    }

    @GetMapping("/distribusi-jumlah-dosen-tetap-dan-tidak-tetap-berdasarkan-umur")
    @Secured("ROLE_AUTHORIZED")
    public String distribusiJumlahDosenTetapDanTidakTetapBerdasarkanUmur(Model uiModel) {
        uiModel.addAttribute("uriBackend", "\"/get-distribusi-jumlah-dosen-tetap-dan-tidak-tetap-berdasarkan-umur\"");
        uiModel.addAttribute("chartForm", "\"Bar Chart\"");
        uiModel.addAttribute("config", "'{\"rows\":[\"umur\"], " +
            "\"cols\":[\"status_dosen\"],"+
            "\"vals\":[\"total_employee\"],\"aggregatorName\":\"Sum\"}'");
        uiModel.addAttribute("menu", "SDM");
        uiModel.addAttribute("title", "Distribusi Dosen Tetap dan Tidak Tetap Berdasarkan Umur");

        return "pivot-table";
    }

    @GetMapping("/distribusi-jumlah-dosen-tetap-berdasarkan-golongan")
    @Secured("ROLE_AUTHORIZED")
    public String distribusiJumlahDosenTetapBerdasarkanGolongan(Model uiModel) {
        uiModel.addAttribute("uriBackend", "\"/get-distribusi-jumlah-dosen-tetap-berdasarkan-golongan\"");
        uiModel.addAttribute("chartForm", "\"Bar Chart\"");
        uiModel.addAttribute("config", "'{\"rows\":[\"golongan\"], " +
            "\"cols\":[\"status_dosen\"],"+
            "\"vals\":[\"total_employee\"],\"aggregatorName\":\"Sum\"}'");
        uiModel.addAttribute("menu", "SDM");
        uiModel.addAttribute("title", "Distribusi Dosen Tetap Berdasarkan Golongan");

        return "pivot-table";
    }

    @GetMapping("/ratio-jumlah-dosen-dan-mahasiswa")
    @Secured("ROLE_AUTHORIZED")
    public String ratioJumlahDosenDanMahasiswa(Model uiModel) {
        String configAgg = "'{\"rows\":[\"fakultas\", \"semester\"], " +
            "\"cols\":[\"tahun\"],"+
            "\"vals\":[],\"aggregatorName\":\"Multifact Aggregators\","+
            "\"rendererOptions\": {aggregations : { defaultAggregations : aggMap, }},}'";

        String config = "'{\"rows\":[\"semester\", \"fakultas\"], " +
            "\"cols\":[\"tahun\"],"+
            "\"aggregatorName\":\"Multifact Aggregators\"}'";

        String aggMap = "'agg1': { aggType: 'Sum', arguments: ['jumlah_dosen'], name: 'Jumlah Dosen'," +
            " varName :'a', hidden : false, renderEnhancement : 'barchart' },";
        String aggMap2 = " 'agg2': { aggType: 'Sum', arguments: ['jumlah_mahasiswa'], name: 'Jumlah Mahasiswa'," +
            " varName :'b', hidden : false, renderEnhancement : 'barchart' },";

        String aggAll = "{" + aggMap + aggMap2 + "}";
        uiModel.addAttribute("uriBackend", "\"/get-ratio-jumlah-dosen-dan-mahasiswa\"");
        uiModel.addAttribute("chartForm", "\"GT Table Heatmap and Barchart\"");
        uiModel.addAttribute("config", config);
        uiModel.addAttribute("aggMapBackend", aggAll);
        uiModel.addAttribute("menu", "SDM");
        uiModel.addAttribute("title", "Ratio Jumlah Dosen dan Mahasiswa");
        uiModel.addAttribute("css", ".pvtTotalLabel, .rowTotal, .pvtGrandTotal, .pvtMeasureTotalLabel { display: none; } .pvtTotalLabel, .colTotal, .pvtGrandTotal, .pvtMeasureTotalLabel { display: none; }");
        return "pivot-table-multifact";
    }

    @GetMapping("/distribusi-jumlah-dosen-tetap-dan-tidak-tetap-berdasarkan-fakultas-prodi-jurusan")
    @Secured("ROLE_AUTHORIZED")
    public String disribusiJumlahDosenTetapDanTidakTetapBerdasarkanFakultasProdiJurusan(Model uiModel) {
        uiModel.addAttribute("uriBackend", "\"/get-distribusi-jumlah-dosen-tetap-dan-tidak-tetap-berdasarkan-fakultas-prodi-jurusan\"");
        uiModel.addAttribute("chartForm", "\"Bar Chart\"");
        uiModel.addAttribute("config", "'{\"rows\":[\"fakultas\", \"jurusan\", \"program_studi\"], " +
            "\"cols\":[\"status_dosen\"],"+
            "\"vals\":[\"total_employee\"],\"aggregatorName\":\"Sum\"}'");
        uiModel.addAttribute("menu", "SDM");
        uiModel.addAttribute("title", "Distribusi Dosen Tetap dan Tidak Tetap Berdasarkan Fakultas/Jurusan/Program Studi");

        return "pivot-table";
    }

    @GetMapping("/modul-bidang-sdm")
    @Secured("ROLE_AUTHORIZED")
    public String sdm(Model uiModel) {
        uiModel.addAttribute("uriBackend", "\"/get-dosen-list\"");
        uiModel.addAttribute("chartForm", "\"Stacked Bar Chart\"");
        uiModel.addAttribute("config", "'{\"rows\":[\"fakultas\"], " +
            "\"cols\":[\"status_dosen\"],"+
            "\"aggregatorName\":\"Count\"}'");
        uiModel.addAttribute("menu", "SDM");
        uiModel.addAttribute("title", "Modul Bidang SDM");
        return "pivot-table";
    }
    @GetMapping("/jumlah-karyawan")
    @Secured("ROLE_AUTHORIZED")
    public String karyawan(Model uiModel) {
        uiModel.addAttribute("uriBackend", "\"/get-jumlah-karyawan\"");
        uiModel.addAttribute("chartForm", "\"Bar Chart\"");
        uiModel.addAttribute("config", "'{\"rows\":[], " +
            "\"cols\":[\"tahun\"],"+
            "\"vals\":[\"total_employee\"],\"aggregatorName\":\"Sum\"}'");
        uiModel.addAttribute("menu", "SDM");
        uiModel.addAttribute("title", "Perkembangan Jumlah Karyawan");

        return "pivot-table";
    }
    @GetMapping("/jumlah-dosen-tetap-tidak-tetap")
    @Secured("ROLE_AUTHORIZED")
    public String dosen(Model uiModel) {
        uiModel.addAttribute("uriBackend", "\"/get-jumlah-dosen-tetap-tidak-tetap\"");
        uiModel.addAttribute("chartForm", "\"Bar Chart\"");
        uiModel.addAttribute("config", "'{\"rows\":[\"status_dosen\"], " +
            "\"cols\":[\"tahun\"],"+
            "\"vals\":[\"total_employee\"],\"aggregatorName\":\"Sum\"}'");
        uiModel.addAttribute("menu", "SDM");
        uiModel.addAttribute("title", "Distribusi Jumlah Dosen Tetap dan Tidak Tetap");

        return "pivot-table";
    }
}
