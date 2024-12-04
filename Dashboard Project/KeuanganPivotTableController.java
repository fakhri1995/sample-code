package com.unhan.dashboard.controller;

import com.unhan.dashboard.dto.euisapi.RekapDataString;
import com.unhan.dashboard.dto.euisapi.TermsApiResponse;
import com.unhan.dashboard.service.PenelitianService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class KeuanganPivotTableController {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeuanganPivotTableController.class);
    private static final String TERMS_URI_FMT = "%s/api/terms";

    @Value("${euis.domain}")
    private String euisDomain;

    @Autowired
    PenelitianService penelitianService;

    @GetMapping("/modul-bidang-keuangan")
    @Secured("ROLE_AUTHORIZED")
    public String modulBidangKeuangan(Model uiModel) throws Exception {
        String configAgg = "'{\"rows\":[\"tahun\"], " +
            "\"cols\":[],"+
            "\"vals\":[],\"aggregatorName\":\"Multifact Aggregators\","+
            "\"rendererOptions\": {aggregations : { defaultAggregations : aggMap, }},}'";

        String config = "'{\"rows\":[\"tahun\"], " +
            "\"cols\":[],"+
            "\"vals\":[],\"aggregatorName\":\"Multifact Aggregators\"}'";

        String aggMap = "'agg1': { aggType: 'Sum', arguments: ['total_anggaran'], name: 'Total Anggaran'," +
            " varName :'a', hidden : false, renderEnhancement : 'barchart' },";
        String aggMap2 = " 'agg2': { aggType: 'Sum', arguments: ['alokasi'], name: 'Alokasi'," +
            " varName :'b', hidden : false, renderEnhancement : 'barchart' },";
        String aggMap3 = " 'agg3': { aggType: 'Sum', arguments: ['sisa'], name: 'Sisa'," +
            " varName :'c', hidden : false, renderEnhancement : 'barchart' },";
        String aggMap4 = " 'agg4': { aggType: 'Sum', arguments: ['realisasi'], name: 'Realisasi'," +
            " varName :'d', hidden : false, renderEnhancement : 'barchart' },";


        String aggAll = "{" + aggMap + aggMap2 + aggMap3 + aggMap4 + "}";
        uiModel.addAttribute("uriBackend", "\"/get-realisasi-anggaran\"");
        uiModel.addAttribute("chartForm", "\"GT Table Heatmap and Barchart\"");
        uiModel.addAttribute("config", config);
        uiModel.addAttribute("aggMapBackend", aggAll);
        uiModel.addAttribute("menu", "Keuangan");
        uiModel.addAttribute("title", "Modul Bidang Keuangan");
        uiModel.addAttribute("main", true);

        return "pivot-table-multifact";
    }

    private List<String> createTermsFromAPIResponses(Collection<RekapDataString> responses) {
        if (responses == null || responses.isEmpty()) {
            return Collections.emptyList();
        }

        return responses.stream()
            .map(RekapDataString::getValue)
            .collect(Collectors.toList());
    }

    @GetMapping("/pemasukan-dan-pengeluaran-universitas")
    @Secured("ROLE_AUTHORIZED")
    public String pemasukanDanPengeluaranUniversitas(Model uiModel) throws Exception {
        String configAgg = "'{\"rows\":[\"tahun\"], " +
            "\"cols\":[],"+
            "\"vals\":[],\"aggregatorName\":\"Multifact Aggregators\","+
            "\"rendererOptions\": {aggregations : { defaultAggregations : aggMap, }}," +
            "\"hiddenAttributes\": [\"pemasukan\"],}'";

        String config = "'{\"rows\":[\"tahun\"], " +
            "\"aggregatorName\":\"Multifact Aggregators\", "+"\"hiddenAttributes\": [\"pemasukan\"]"+"}'";

//        String aggMap = "'agg1': { aggType: 'Sum', arguments: ['pemasukan'], name: 'Pemasukan'," +
//                    " varName :'a', hidden : false, renderEnhancement : 'barchart' },";
        String aggMap2 = " 'agg21': { aggType: 'Sum', arguments: ['pengeluaran'], name: 'Pengeluaran'," +
            " varName :'b', hidden : false, renderEnhancement : 'barchart' },";

        String aggAll = "{" + aggMap2 + "}";
        uiModel.addAttribute("uriBackend", "\"/get-pemasukan-dan-pengeluaran-universitas\"");
        uiModel.addAttribute("chartForm", "\"GT Table Heatmap and Barchart\"");
        uiModel.addAttribute("config", config);
        uiModel.addAttribute("aggMapBackend", aggAll);
        uiModel.addAttribute("menu", "Keuangan");
        uiModel.addAttribute("title", "Pengeluaran Universitas");

        return "pivot-table-multifact";
    }

    @GetMapping("/realisasi-anggaran")
    @Secured("ROLE_AUTHORIZED")
    public String realisasiAnggaran(Model uiModel) throws Exception {
        String configAgg = "'{\"rows\":[\"tahun\"], " +
            "\"cols\":[],"+
            "\"vals\":[],\"aggregatorName\":\"Multifact Aggregators\","+
            "\"rendererOptions\": {aggregations : { defaultAggregations : aggMap, }},}'";

        String config = "'{\"rows\":[\"tahun\"], " +
            "\"cols\":[\"unit_kerja\"],"+
            "\"vals\":[],\"aggregatorName\":\"Multifact Aggregators\"}'";

        String aggMap = "'agg1': { aggType: 'Sum', arguments: ['total_anggaran'], name: 'Total Anggaran'," +
            " varName :'a', hidden : false, renderEnhancement : 'barchart' },";
        String aggMap2 = " 'agg2': { aggType: 'Sum', arguments: ['alokasi'], name: 'Alokasi'," +
            " varName :'b', hidden : false, renderEnhancement : 'barchart' },";
        String aggMap3 = " 'agg3': { aggType: 'Sum', arguments: ['sisa'], name: 'Sisa'," +
            " varName :'c', hidden : false, renderEnhancement : 'barchart' },";
        String aggMap4 = " 'agg4': { aggType: 'Sum', arguments: ['realisasi'], name: 'Realisasi'," +
            " varName :'d', hidden : false, renderEnhancement : 'barchart' },";


        String aggAll = "{" + aggMap + aggMap2 + aggMap3 + aggMap4 + "}";
        uiModel.addAttribute("uriBackend", "\"/get-realisasi-anggaran\"");
        uiModel.addAttribute("chartForm", "\"GT Table Heatmap and Barchart\"");
        uiModel.addAttribute("config", config);
        uiModel.addAttribute("aggMapBackend", aggAll);
        uiModel.addAttribute("menu", "Keuangan");
        uiModel.addAttribute("title", "Realisasi Anggaran");

        return "pivot-table-multifact";
    }

    @GetMapping("/bidang-kemahasiswaan")
    @Secured("ROLE_AUTHORIZED")
    public String bidangKemahasiswaan(Model uiModel) throws Exception {
        uiModel.addAttribute("uriBackend", "\"/get-distribusi-beasiswa\"");
        uiModel.addAttribute("chartForm", "\"Bar Chart\"");
        uiModel.addAttribute("config", "'{\"rows\":[\"tahun\"], " +
            "\"cols\":[\"fakultas\"],"+
            "\"vals\":[\"jumlah_penerima\"],\"aggregatorName\":\"Sum\"}'");
        uiModel.addAttribute("menu", "Keuangan");
        uiModel.addAttribute("title", "Bidang Kemahasiswaan (Rekap Mahasiswa Penerima Beasiswa Per Fakultas)");

        return "pivot-table";
    }

    @GetMapping("/perkembangan-jumlah-mahasiswa-penerima-beasiswa")
    @Secured("ROLE_AUTHORIZED")
    public String perkembanganJumlahMahasiswaPenerimaBeasiswa(Model uiModel) throws Exception {
        uiModel.addAttribute("uriBackend", "\"/get-perkembangan-jumlah-mahasiswa-penerima-beasiswa\"");
        uiModel.addAttribute("chartForm", "\"Bar Chart\"");
        uiModel.addAttribute("config", "'{\"rows\":[\"tahun\"], " +
            "\"cols\":[],"+
            "\"vals\":[\"jumlah_penerima\"],\"aggregatorName\":\"Sum\"}'");
        uiModel.addAttribute("menu", "Keuangan");
        uiModel.addAttribute("title", "Perkembangan Jumlah Mahasiswa Penerima Beasiswa");

        return "pivot-table";
    }

    @GetMapping("/perkembangan-jumlah-mahasiswa-yang-ditawarkan")
    @Secured("ROLE_AUTHORIZED")
    public String perkembanganJumlahMahasiswaYangDitawarkan(Model uiModel) throws Exception {
        String configAgg = "'{\"rows\":[\"tahun\"], " +
            "\"cols\":[],"+
            "\"vals\":[],\"aggregatorName\":\"Multifact Aggregators\","+
            "\"rendererOptions\": {aggregations : { defaultAggregations : aggMap, }},}'";

        String config = "'{\"rows\":[\"semester\"], " +
            "\"cols\":[\"tahun\"],"+
            "\"vals\":[],\"aggregatorName\":\"Multifact Aggregators\"}'";

        String aggMap = "'agg1': { aggType: 'Sum', arguments: ['jumlah_ditawarkan'], name: 'Jumlah Mahasiswa yang Ditawarkan Beasiswa'," +
            " varName :'a', hidden : false, renderEnhancement : 'barchart'  },";
        String aggMap2 = " 'agg2': { aggType: 'Sum', arguments: ['jumlah_mahasiswa'], name: 'Jumlah Mahasiswa Total'," +
            " varName :'b', hidden : false, renderEnhancement : 'barchart'  },";

        String aggAll = "{" + aggMap + aggMap2 + "}";
        uiModel.addAttribute("uriBackend", "\"/get-perkembangan-jumlah-mahasiswa-yang-ditawarkan\"");
        uiModel.addAttribute("chartForm", "\"GT Table Heatmap and Barchart\"");
        uiModel.addAttribute("config", config);
        uiModel.addAttribute("aggMapBackend", aggAll);
        uiModel.addAttribute("menu", "Keuangan");
        uiModel.addAttribute("title", "Perkembangan Jumlah Mahasiswa yang Ditawarkan");
        uiModel.addAttribute("css", ".pvtTotalLabel, .rowTotal, .pvtGrandTotal, .pvtMeasureTotalLabel { display: none; } .pvtTotalLabel, .colTotal, .pvtGrandTotal, .pvtMeasureTotalLabel { display: none; }");


        return "pivot-table-multifact";
    }

    @GetMapping("/distribusi-beasiswa")
    @Secured("ROLE_AUTHORIZED")
    public String distribusiBeasiswa(Model uiModel) throws Exception {
        uiModel.addAttribute("uriBackend", "\"/get-distribusi-beasiswa\"");
        uiModel.addAttribute("chartForm", "\"Stacked Bar Chart\"");
        uiModel.addAttribute("config", "'{\"rows\":[\"fakultas\", \"jurusan\", \"program_studi\"], " +
            "\"cols\":[\"tahun\"],"+
            "\"vals\":[\"jumlah_penerima\"],\"aggregatorName\":\"Sum\"}'");
        uiModel.addAttribute("menu", "Keuangan");
        uiModel.addAttribute("title", "Distribusi Beasiswa per Fakultas / Jurusan / Program Studi");

        return "pivot-table";
    }

}
