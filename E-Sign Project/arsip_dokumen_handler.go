package handler

import (
	"e-sign-backend/entity"
	"e-sign-backend/request"
	"e-sign-backend/response"
	"e-sign-backend/service"
	"e-sign-backend/util"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"strconv"

	"github.com/gin-gonic/gin"
)

type arsipDokumenHandler struct {
	arsipDokumenService service.ArsipDokumenService
	mailService         service.MailService
	penggunaService     service.PenggunaService
}

func NewArsipDokumenHandler(arsipDokumenService service.ArsipDokumenService, mailService service.MailService, penggunaService service.PenggunaService) *arsipDokumenHandler {
	return &arsipDokumenHandler{arsipDokumenService: arsipDokumenService, mailService: mailService, penggunaService: penggunaService}
}

func (h *arsipDokumenHandler) GetOrganisasiDropdown(c *gin.Context) {
	organisasiDropdown, err := h.arsipDokumenService.GetOrganisasiDropdown()

	if err != nil {
		util.APIListResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil, 0)
		return
	}
	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", organisasiDropdown, uint(len(organisasiDropdown)))
}

func (h *arsipDokumenHandler) GetJenisDokumenDropdown(c *gin.Context) {
	result, err := h.arsipDokumenService.GetJenisDokumenDropdown()

	if err != nil {
		util.APIListResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil, 0)
		return
	}
	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", result, uint(len(result)))
}

func (h *arsipDokumenHandler) GetStatusDokumenDropdown(c *gin.Context) {
	result, err := h.arsipDokumenService.GetStatusDokumenDropdown()

	if err != nil {
		util.APIListResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil, 0)
		return
	}
	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", result, uint(len(result)))
}

func (h *arsipDokumenHandler) GetAllArsipDokumen(c *gin.Context) {
	nomor_pengajuan := c.Query("nomor_pengajuan")
	nama_pengaju := c.Query("nama_pengaju")
	nama_penandatangan := c.Query("nama_penandatangan")
	pengaju_login := c.Query("pengaju_login")
	sign_page, err := strconv.ParseBool(c.Query("sign_page"))
	if err != nil {
		util.APIListResponse(c, "Error parsing sign_page", http.StatusBadRequest, "Error parsing sign_page", nil, 0)
		return
	}
	id_jenis_dokumen, err := strconv.ParseInt(c.Query("id_jenis_dokumen"), 0, 64)
	if err != nil {
		util.APIListResponse(c, "Error parsing id_jenis_dokumen", http.StatusBadRequest, "Error parsing id_jenis_dokumen", nil, 0)
		return
	}
	tanggal_pengajuan := c.Query("tanggal_pengajuan")
	id_status_dokumen, err := strconv.ParseInt(c.Query("id_status_dokumen"), 0, 64)
	if err != nil {
		util.APIListResponse(c, "Error parsing id_status_dokumen", http.StatusBadRequest, "Error parsing id_status_dokumen", nil, 0)
		return
	}
	penandatangan_login, err := strconv.ParseInt(c.Query("id_pengguna"), 0, 64)
	if err != nil {
		util.APIListResponse(c, "Error parsing id_pengguna", http.StatusBadRequest, "Error parsing id_pengguna", nil, 0)
		return
	}
	result, err := h.arsipDokumenService.GetAllArsipDokumen(nomor_pengajuan, nama_penandatangan, nama_pengaju, id_jenis_dokumen,
		tanggal_pengajuan, id_status_dokumen, penandatangan_login, pengaju_login, sign_page)

	if err != nil {
		util.APIListResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil, 0)
		return
	}
	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", result, uint(len(result)))
}

func (h *arsipDokumenHandler) GetArsipSignList(c *gin.Context) {
	nomor_surat := c.Query("nomor_surat")
	username_pengaju := c.Query("username_pengaju")
	id_jenis_dokumen, err := strconv.ParseInt(c.Query("id_jenis_dokumen"), 0, 64)
	if err != nil {
		util.APIListResponse(c, "Error parsing id_jenis_dokumen", http.StatusBadRequest, "Error parsing id_jenis_dokumen", nil, 0)
		return
	}
	tanggal_pengajuan := c.Query("tanggal_pengajuan")
	penandatangan_login, err := strconv.ParseInt(c.Query("id_pengguna"), 0, 64)
	if err != nil {
		util.APIListResponse(c, "Error parsing id_pengguna", http.StatusBadRequest, "Error parsing id_pengguna", nil, 0)
		return
	}
	if penandatangan_login == -1 {
		util.APIListResponse(c, "Penandatangan tidak terdaftar", http.StatusBadRequest, "Error id_penandatangan", nil, 0)
		return
	}
	// result, err := h.arsipDokumenService.GetAllArsipDokumen(nomor_pengajuan, nama_penandatangan, nama_pengaju, id_jenis_dokumen,
	// 	tanggal_pengajuan, id_status_dokumen, penandatangan_login, pengaju_login, sign_page)
	result, err := h.arsipDokumenService.GetArsipSignList(nomor_surat, username_pengaju, id_jenis_dokumen, tanggal_pengajuan, penandatangan_login)

	if err != nil {
		util.APIListResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil, 0)
		return
	}
	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", result, uint(len(result)))
}

// func (h *arsipDokumenHandler) GetArsipSignListBatch(c *gin.Context) {
// 	nomor_surat := c.Query("nomor_surat")
// 	username_pengaju := c.Query("username_pengaju")
// 	batchId, err := strconv.Atoi(c.Query("batch_id"))
// 	if err != nil {
// 		util.APIListResponse(c, "Error parsing batch_id", http.StatusBadRequest, "Error parsing batch_id", nil, 0)
// 		return
// 	}
// 	id_jenis_dokumen, err := strconv.ParseInt(c.Query("id_jenis_dokumen"), 0, 64)
// 	if err != nil {
// 		util.APIListResponse(c, "Error parsing id_jenis_dokumen", http.StatusBadRequest, "Error parsing id_jenis_dokumen", nil, 0)
// 		return
// 	}
// 	tanggal_pengajuan := c.Query("tanggal_pengajuan")
// 	penandatangan_login, err := strconv.ParseInt(c.Query("id_pengguna"), 0, 64)
// 	if err != nil {
// 		util.APIListResponse(c, "Error parsing id_pengguna", http.StatusBadRequest, "Error parsing id_pengguna", nil, 0)
// 		return
// 	}
// 	if penandatangan_login == -1 {
// 		util.APIListResponse(c, "Penandatangan tidak terdaftar", http.StatusBadRequest, "Error id_penandatangan", nil, 0)
// 		return
// 	}
// 	// result, err := h.arsipDokumenService.GetAllArsipDokumen(nomor_pengajuan, nama_penandatangan, nama_pengaju, id_jenis_dokumen,
// 	// 	tanggal_pengajuan, id_status_dokumen, penandatangan_login, pengaju_login, sign_page)
// 	result, err := h.arsipDokumenService.GetArsipSignListBatch(nomor_surat, username_pengaju, id_jenis_dokumen, tanggal_pengajuan, penandatangan_login, batchId)

// 	if err != nil {
// 		util.APIListResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil, 0)
// 		return
// 	}
// 	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", result, uint(len(result)))
// }

func (h *arsipDokumenHandler) GetArsipRejectList(c *gin.Context) {
	username_pengaju := c.Query("username_pengaju")
	result, err := h.arsipDokumenService.GetArsipRejectList(username_pengaju)

	if err != nil {
		util.APIListResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil, 0)
		return
	}
	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", result, uint(len(result)))
}

func (h *arsipDokumenHandler) GetConfirmedOrDeniedRejectArsip(c *gin.Context) {
	id_penandatangan_login, err := strconv.Atoi(c.Query("id_penandatangan_login"))
	if err != nil {
		util.APIListResponse(c, "Error parsing id_pengguna", http.StatusBadRequest, "Error parsing id_pengguna", nil, 0)
		return
	}
	result, err := h.arsipDokumenService.GetConfirmedOrDeniedRejectArsip(id_penandatangan_login)

	if err != nil {
		util.APIListResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil, 0)
		return
	}
	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", result, uint(len(result)))
}

func (h *arsipDokumenHandler) GetArsipList(c *gin.Context) {
	username_pengaju_login := c.Query("username_pengaju_login")
	id_penandatangan_login, err := strconv.Atoi(c.Query("id_penandatangan_login"))
	if err != nil {
		util.APIListResponse(c, "Error parsing id_pengguna", http.StatusBadRequest, "Error parsing id_pengguna", nil, 0)
		return
	}
	nama_penandatangan := c.Query("nama_penandatangan")
	username_pengaju := c.Query("username_pengaju")
	nomor_surat := c.Query("nomor_surat")
	tanggal_pengajuan := c.Query("tanggal_pengajuan")

	id_jenis_dokumen, err := strconv.Atoi(c.Query("id_jenis_dokumen"))
	if err != nil {
		util.APIListResponse(c, "Error parsing id_jenis_dokumen", http.StatusBadRequest, "Error parsing id_jenis_dokumen", nil, 0)
		return
	}

	id_status_dokumen, err := strconv.Atoi(c.Query("id_status_dokumen"))
	if err != nil {
		util.APIListResponse(c, "Error parsing id_status_dokumen", http.StatusBadRequest, "Error parsing id_status_dokumen", nil, 0)
		return
	}

	nomor_halaman, err := strconv.Atoi(c.Query("nomor_halaman"))
	if err != nil {
		util.APIListResponse(c, "Error parsing nomor_halaman", http.StatusBadRequest, "Error parsing nomor_halaman", nil, 0)
		return
	}

	total_data, err := strconv.ParseInt(c.Query("total_data"), 10, 64)
	if err != nil {
		util.APIListResponse(c, "Error parsing total_data", http.StatusBadRequest, "Error parsing total_data", nil, 0)
		return
	}

	result, totalData, err := h.arsipDokumenService.GetArsipList(username_pengaju_login, id_penandatangan_login, nama_penandatangan,
		username_pengaju, nomor_surat, tanggal_pengajuan, id_jenis_dokumen, id_status_dokumen, nomor_halaman, total_data)

	if err != nil {
		util.APIListResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil, 0)
		return
	}
	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", result, uint(totalData))
}

// func (h *arsipDokumenHandler) GetPemaraf(c *gin.Context) {
// 	//tipe_ttd = 1 untuk pemaraf, tipe_ttd = 2 untuk penandatangan
// 	tipe_ttd, err := strconv.ParseInt(c.Query("tipe_ttd"), 0, 64)
// 	if err != nil {
// 		util.APIListResponse(c, "Error parsing tipe_ttd", http.StatusBadRequest, "Error parsing tipe_ttd", nil, 0)
// 		return
// 	}
// 	id_jenis_dokumen_organisasi, err := strconv.ParseInt(c.Query("id_jenis_dokumen_organisasi"), 0, 64)
// 	if err != nil {
// 		util.APIListResponse(c, "Error parsing id_jenis_dokumen_organisasi", http.StatusBadRequest, "Error parsing id_jenis_dokumen_organisasi", nil, 0)
// 		return
// 	}
// 	pemaraf, err := h.arsipDokumenService.GetPemaraf(int(tipe_ttd), int(id_jenis_dokumen_organisasi))

// 	if err != nil {
// 		util.APIListResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil, 0)
// 		return
// 	}
// 	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", pemaraf, uint(len(pemaraf)))

// }

func (h *arsipDokumenHandler) GetDropdownPemaraf(c *gin.Context) {
	pemarafDropdown, err := h.arsipDokumenService.GetDropdownPemaraf()

	if err != nil {
		util.APIListResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil, 0)
		return
	}
	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", pemarafDropdown, uint(len(pemarafDropdown)))
}

func (h *arsipDokumenHandler) GetJenisTtd(c *gin.Context) {
	jenisTtd, err := h.arsipDokumenService.GetJenisTtd()

	if err != nil {
		util.APIListResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil, 0)
		return
	}
	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", jenisTtd, uint(len(jenisTtd)))
}

func (h *arsipDokumenHandler) GetDisclaimerBsre(c *gin.Context) {
	disclaimer, disclaimerFontSize, err := h.arsipDokumenService.GetDisclaimerBsre(c)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil)
		return
	}
	result := response.DisclaimerBSREResponse{
		Disclaimer:         disclaimer.Value,
		DisclaimerFontSize: disclaimerFontSize,
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", result)
}

func (h *arsipDokumenHandler) GetDokumen(c *gin.Context) {
	// var data request.FormFileRequest
	var err error
	field := c.Query("field")
	value := c.Query("value")
	tipe := c.Query("tipe")
	err = h.arsipDokumenService.GetDokumen(c, field, value, tipe)
	if err != nil {
		log.Println(err)
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "File tidak ditemukan", nil)
		return
	}

	util.APIResponse(c, "Retrieve Data success!", http.StatusOK, "ok", nil)
}

func (h *arsipDokumenHandler) DeleteTemporaryFile(c *gin.Context) {
	file_name := c.Param("file_name")
	err := h.arsipDokumenService.DeleteTemporaryFile(file_name)

	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", file_name)
}

func (h *arsipDokumenHandler) CreateArsip(c *gin.Context) {
	var req request.ArsipDokumenRequest
	err := c.ShouldBind(&req)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error Binding", nil)
		return
	}
	err = req.ArsipDokumen.Validate()
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error Validate", nil)
		return
	}
	// fmt.Println("ttd ", req.ArsipDokumen.TtdArsipDokumen)
	var res entity.ArsipDokumen
	res, err = h.arsipDokumenService.CreateArsip(c, req.ArsipDokumen, h.mailService, req.KodeOrganisasi, req.AlurPersetujuan, req.NamaJenisDokumen,
		req.PdfSource, req.ImgParameter)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}
	util.APIResponse(c, "Save data success!", http.StatusOK, "ok", res)
}

func (h *arsipDokumenHandler) CreateExternalArsip(c *gin.Context) {
	var req request.ExternalArsipDokumenRequest
	var tempTtd request.ExternalArsipDokumenRequest
	var tempJenis request.ExternalArsipDokumenRequest

	err := c.ShouldBind(&req)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error JSON", nil)
		return
	}

	if c.PostForm("Ttd") != "" {
		err = json.Unmarshal([]byte(c.PostForm("Ttd")), &tempTtd)
		if err != nil {
			util.APIResponse(c, err.Error(), http.StatusBadRequest, fmt.Sprintf("error unmarshalling ttd: %v", err), nil)
			return
		}
		req.TtdArsipDokumen = tempTtd.TtdArsipDokumen
	}

	if c.PostForm("JenisTtd") != "" {
		err = json.Unmarshal([]byte(c.PostForm("JenisTtd")), &tempJenis)
		if err != nil {
			util.APIResponse(c, err.Error(), http.StatusBadRequest, fmt.Sprintf("error unmarshalling jenis ttd: %v", err), nil)
			return
		}
		req.JenisTtdArsipDokumen = tempJenis.JenisTtdArsipDokumen
	}

	var res entity.ArsipDokumen
	res, err = h.arsipDokumenService.TransformInput(c, req, h.mailService)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}
	util.APIResponse(c, "Save data success!", http.StatusOK, "ok", res)
}

func (h *arsipDokumenHandler) AturUlangPengajuan(c *gin.Context) {
	// url := os.Getenv("APP_URL") + "/ext/submit"
	// id_jenis_dokumen_organisasi, err := strconv.ParseUint(c.PostForm("IdJenisDokumenOrganisasi"), 10, 64)
	var req request.ExternalArsipDokumenRequest
	var tempTtd request.ExternalArsipDokumenRequest
	err := c.ShouldBind(&req)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error JSON", nil)
		return
	}

	// header, err := c.FormFile("file")
	// if err != nil {
	// 	errMesage := err.Error()
	// 	if err == http.ErrMissingFile {
	// 		errMesage = "file is missing"
	// 	}
	// 	util.APIResponse(c, errMesage, http.StatusBadRequest, "error", nil)
	// 	return
	// }
	// fmt.Println("file name", header.Filename)

	if req.PenggunaEntry == "" {
		util.APIResponse(c, "PenggunaEntry is empty", http.StatusBadRequest, "error", nil)
		return
	}

	if c.PostForm("Ttd") != "" {
		err = json.Unmarshal([]byte(c.PostForm("Ttd")), &tempTtd)
		if err != nil {
			util.APIResponse(c, err.Error(), http.StatusBadRequest, fmt.Sprintf("error unmarshalling ttd: %v", err), nil)
			return
		}
		req.TtdArsipDokumen = tempTtd.TtdArsipDokumen
		// for _, i := range req.TtdArsipDokumen {
		// 	i.Koordinat = ""
		// 	i.Halaman = 0
		// }
	}

	result, err := h.arsipDokumenService.AturUlangPengajuan(c, req)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", result)
	// id := strconv.Itoa(int(req.IdJenisDokumenOrganisasi))
	// url += "?id_jenis_dokumen_organisasi=" + id + "&source=" + req.SourceApk + "&pengguna_entry=" + req.PenggunaEntry + "&token=" + req.Token
	// log.Println("url :" + url)
	// c.Redirect(http.StatusMovedPermanently, url)
}

func (h *arsipDokumenHandler) RedirectPengajuan(c *gin.Context) {
	// id_jenis_dokumen_organisasi, _ := strconv.ParseUint(c.Query("id_jenis_dokumen_organisasi"), 0, 64)
	id_jenis_dokumen_organisasi := c.Query("id_jenis_dokumen_organisasi")
	source := c.Query("source")
	pengguna_entry := c.Query("pengguna_entry")
	file_name := c.Query("file_name")
	token := c.Query("token")

	url := os.Getenv("APP_URL") + "/ext/submit"
	// id := strconv.Itoa(id_jenis_dokumen_organisasi)
	url += "?id=" + id_jenis_dokumen_organisasi + "&source=" + source + "&pengguna_entry=" + pengguna_entry + "&file_name=" + file_name + "&token=" + token
	log.Println("url :" + url)

	// if err != nil {
	// 	util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
	// 	return
	// }
	c.Redirect(http.StatusFound, url)
}

func (h *arsipDokumenHandler) FindArsipById(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 0, 64)
	result, err := h.arsipDokumenService.FindArsipById(id)
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", result)
}

func (h *arsipDokumenHandler) FindTempTtdOverrideByCode(c *gin.Context) {
	code := c.Param("code")
	result, err := h.arsipDokumenService.FindTempTtdOverrideByCode(code)
	if err != nil {
		util.APIListResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil, 0)
		return
	}
	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", result, uint(len(result)))
}

func (h *arsipDokumenHandler) DeleteTempTtdOverrideByCode(c *gin.Context) {
	code := c.Param("code")
	err := h.arsipDokumenService.DeleteTempTtdOverrideByCode(code)

	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", nil)
}

func (h *arsipDokumenHandler) GetListEligibleIdPengguna(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 0, 64)
	result, err := h.arsipDokumenService.GetListEligibleIdPengguna(id)
	if err != nil {
		util.APIListResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil, 0)
		return
	}
	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", result, uint(len(result)))
}

func (h *arsipDokumenHandler) GetOrganisasiByPenggunaId(c *gin.Context) {
	id_pengguna, _ := strconv.ParseUint(c.Param("id_pengguna"), 0, 64)
	result, err := h.arsipDokumenService.GetOrganisasiByPenggunaId(id_pengguna)
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", result)
}

func (h *arsipDokumenHandler) TestEmail(c *gin.Context) {
	err := h.arsipDokumenService.TestEmail(h.mailService)
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", nil)
}

func (h *arsipDokumenHandler) GenerateNomorPengajuan(c *gin.Context) {
	fmt.Println("request", c.Request.Header.Get("Origin"))
	kode_org := c.Query("kode_org")
	nomor_pengajuan, err := h.arsipDokumenService.GenerateNomorPengajuan(kode_org)
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", nomor_pengajuan)
}

func (h *arsipDokumenHandler) GetPenggunaFromArsip(c *gin.Context) {
	id_arsip_dokumen, _ := strconv.ParseUint(c.Query("id_arsip_dokumen"), 0, 64)
	tipe_ttd, _ := strconv.ParseInt(c.Query("tipe_ttd"), 0, 64)
	urutan_ttd, _ := strconv.ParseInt(c.Query("urutan_ttd"), 0, 64)
	signer, err := h.arsipDokumenService.GetPenggunaFromArsip(id_arsip_dokumen, int(tipe_ttd), int(urutan_ttd))
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", signer)
}

func (h *arsipDokumenHandler) GetDetailArsipDokumen(c *gin.Context) {
	id_jenis_dokumen_organisasi, _ := strconv.ParseUint(c.Query("id_jenis_dokumen_organisasi"), 0, 64)
	id_status_dokumen, _ := strconv.ParseUint(c.Query("id_status_dokumen"), 0, 64)
	organisasi, err := h.arsipDokumenService.FindOrganisasiByIdJDO(id_jenis_dokumen_organisasi)
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	jenisDokumen, err := h.arsipDokumenService.FindJenisDokumenByIdJDO(id_jenis_dokumen_organisasi)
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	tingkatKerahasiaan, err := h.arsipDokumenService.FindTingkatKerahasiaanByIdJDO(id_jenis_dokumen_organisasi)
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	status, err := h.arsipDokumenService.GetStatusDokumen(id_status_dokumen)
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	result := response.DetailArsipDokumenResponse{
		Organisasi:         organisasi,
		JenisDokumen:       jenisDokumen,
		TingkatKerahasiaan: tingkatKerahasiaan,
		Status:             status,
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", result)
}

func (h *arsipDokumenHandler) SignArsipDokumen(c *gin.Context) {
	var req request.SignArsipDokumenRequest
	err := c.ShouldBindJSON(&req)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error JSON", nil)
		return
	}
	err = h.arsipDokumenService.Sign(c, req, h.mailService)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}

	util.APIResponse(c, "Save data success!", http.StatusOK, "ok", nil)
}

func (h *arsipDokumenHandler) ValidateSignArsipDokumenBulk(c *gin.Context) {
	var req []request.SignArsipDokumenRequest
	err := c.ShouldBindJSON(&req)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error JSON", nil)
		return
	}
	err = h.arsipDokumenService.ValidateSignBulk(c, req, h.penggunaService)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}

	util.APIResponse(c, "Save data success!", http.StatusOK, "ok", nil)
}

func (h *arsipDokumenHandler) SignArsipDokumenBulk(c *gin.Context) {
	var req []request.SignArsipDokumenRequest
	err := c.ShouldBindJSON(&req)
	if err != nil {
		fmt.Println("error bind JSON", req)
	}
	err = h.arsipDokumenService.SignBulk(c, req, h.mailService, h.penggunaService)
	if err != nil {
		fmt.Println("error SignBulk", req)
	}
}

func (h *arsipDokumenHandler) GetFinishedListBulk(c *gin.Context) {
	id_penandatangan_login, err := strconv.Atoi(c.Query("id_penandatangan_login"))
	if err != nil {
		util.APIListResponse(c, "Error parsing id_pengguna", http.StatusBadRequest, "Error parsing id_pengguna", nil, 0)
		return
	}
	result, err := h.arsipDokumenService.GetFinishedListBulk(id_penandatangan_login)

	if err != nil {
		util.APIListResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil, 0)
		return
	}
	util.APIListResponse(c, "Retrieve data success!", http.StatusOK, "ok", result, uint(len(result)))
}

func (h *arsipDokumenHandler) RemoveNotificationBulk(c *gin.Context) {
	var req []request.SignArsipDokumenRequest
	err := c.ShouldBindJSON(&req)
	if err != nil {
		fmt.Println("error bind JSON", req)
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error bind JSON", nil)
		return
	}
	fmt.Println("req", req)
	err = h.arsipDokumenService.RemoveNotificationBulk(c, req)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "Error query data", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", nil)
}

// func (h *arsipDokumenHandler) SignArsipDokumenBatch(c *gin.Context) {
// 	var err error
// 	var req request.SignBatchArsipRequest
// 	err = c.ShouldBindJSON(&req)
// 	if err != nil {
// 		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error JSON", nil)
// 		return
// 	}
// 	util.APIResponse(c, "Penandatanganan sedang diproses, notifikasi akan muncul apabila proses telah selesai", http.StatusOK, "ok", nil)
// 	done := make(chan error)
// 	go func() {
// 		cmd := exec.Command("sleep", "5") // Create a command to run the "sleep" command for 60 seconds
// 		err := cmd.Start()
// 		err = h.arsipDokumenService.SignBatch(c, req, h.mailService, h.penggunaService)
// 		if err != nil {
// 			done <- err // Signal the error to the main goroutine
// 			return
// 		}
// 		fmt.Printf("Process started with PID: %d\n", cmd.Process.Pid) // Print the PID of the started process
// 		done <- cmd.Wait()                                            // Wait for the command to finish and signal the result to the main goroutine
// 	}()
// 	// Wait for the goroutine to finish
// 	err = <-done // Receive the result from the goroutine
// 	if err != nil {
// 		fmt.Printf("Error: %v\n", err) // Print any error that occurred during the process execution
// 	}
// 	fmt.Println("Process completed.") // Print a message indicating that the process has completed
// }

func (h *arsipDokumenHandler) VerifyFile(c *gin.Context) {
	verify, err := h.arsipDokumenService.VerifyFile(c)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}

	util.APIResponse(c, "Save data success!", http.StatusOK, "ok", verify)
}

func (h *arsipDokumenHandler) VerifyFileById(c *gin.Context) {
	var req request.VerifyFileRequest
	err := c.ShouldBindJSON(&req)

	verify, err := h.arsipDokumenService.VerifyFileById(req)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}

	util.APIResponse(c, "Save data success!", http.StatusOK, "ok", verify)
}

func (h *arsipDokumenHandler) CancelArsip(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 0, 64)
	err := h.arsipDokumenService.CancelArsip(c, h.mailService, id)

	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}

	util.APIResponse(c, "Pembatalan permohonan berhasil dilakukan!", http.StatusOK, "ok", nil)
}

func (h *arsipDokumenHandler) ConfirmReject(c *gin.Context) {
	var req request.PengajuRequest
	err := c.ShouldBindJSON(&req)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}
	err = h.arsipDokumenService.ConfirmReject(c, h.mailService, req)

	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}

	util.APIResponse(c, "Penolakan berhasil dikonfirm!", http.StatusOK, "ok", nil)
}

func (h *arsipDokumenHandler) DenyReject(c *gin.Context) {
	var req request.DenyRejectRequest
	err := c.ShouldBindJSON(&req)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}

	err = h.arsipDokumenService.DenyReject(c, h.mailService, req)

	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}

	util.APIResponse(c, "Penolakan berhasil disanggah!", http.StatusOK, "ok", nil)
}

func (h *arsipDokumenHandler) UpdateTime(c *gin.Context) {
	id_ttd_arsip_dokumen, _ := strconv.ParseUint(c.Param("id_ttd_arsip_dokumen"), 0, 64)
	err := h.arsipDokumenService.UpdateTime(c, id_ttd_arsip_dokumen)

	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}

	util.APIResponse(c, "Tanggal Update berhasil diubah!", http.StatusOK, "ok", nil)
}

func (h *arsipDokumenHandler) SetNonaktif(c *gin.Context) {
	var req request.NonaktifArsipRequest
	err := c.ShouldBindJSON(&req)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}
	err = h.arsipDokumenService.SetNonaktif(c, req)

	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}

	util.APIResponse(c, "Arsip dokumen berhasil dinonaktifkan!", http.StatusOK, "ok", nil)
}

func (h *arsipDokumenHandler) RejectArsip(c *gin.Context) {
	var req request.RejectRequest
	err := c.ShouldBindJSON(&req)
	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}
	// keterangan := req.Keterangan
	fmt.Println("req", req)
	err = h.arsipDokumenService.RejectArsip(c, h.mailService, req)

	if err != nil {
		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
		return
	}

	util.APIResponse(c, "Penolakan berhasil dilakukan!", http.StatusOK, "ok", nil)
}

// func (h *arsipDokumenHandler) ApiBsreSignTest(c *gin.Context) {
// 	path_file := c.Query("path_file")
// 	id_user_login, _ := strconv.ParseUint(c.Query("id_user_login"), 0, 64)
// 	passphrase := c.Query("passphrase")
// 	_, id_dokumen_bsre, err := h.arsipDokumenService.ApiBsreSignTest(path_file, id_user_login, passphrase)
// 	if err != nil {
// 		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
// 		return
// 	}

// 	util.APIResponse(c, "Save data success!", http.StatusOK, "ok", id_dokumen_bsre)
// }

// func (h *arsipDokumenHandler) ApiBsreSignWithImage(c *gin.Context) {
// 	var req request.SignArsipDokumenRequest
// 	err := c.ShouldBindJSON(&req)
// 	if err != nil {
// 		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error JSON", nil)
// 		return
// 	}
// 	res, err := h.arsipDokumenService.ApiBsreSignWithImage(req.IdArsipDokumen, req.IdTtdArsipDokumen, req.IdUserLogin)
// 	if err != nil {
// 		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
// 		return
// 	}

// 	util.APIResponse(c, "Save data success!", http.StatusOK, "ok", res)
// }

// func (h *arsipDokumenHandler) ApiBsreGetUserStatus(c *gin.Context) {
// 	res, _, err := h.arsipDokumenService.ApiBsreGetUserStatus()

// 	// fmt.Println("res", res)
// 	if err != nil {
// 		util.APIResponse(c, err.Error(), http.StatusBadRequest, "error", nil)
// 		return
// 	}

// 	util.APIResponse(c, "Save data success!", http.StatusOK, "ok", res)
// }

func (h *arsipDokumenHandler) GetStatusByIdArsip(c *gin.Context) {
	id, _ := strconv.ParseUint(c.Param("id"), 0, 64)
	result, err := h.arsipDokumenService.GetStatusByIdArsip(id)
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", result)
}

func (h *arsipDokumenHandler) FindArsipByCodeTransaction(c *gin.Context) {
	code := c.Param("code")
	result, err := h.arsipDokumenService.FindArsipByCodeTransaction(code)
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", result)
}

func (h *arsipDokumenHandler) FindOrganisasiByIdJDO(c *gin.Context) {
	id_jenis_dokumen_organisasi, _ := strconv.ParseUint(c.Param("id_jenis_dokumen_organisasi"), 0, 64)
	result, err := h.arsipDokumenService.FindOrganisasiByIdJDO(id_jenis_dokumen_organisasi)
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", result)
}

func (h *arsipDokumenHandler) FindJenisDokumenByIdJDO(c *gin.Context) {
	id_jenis_dokumen_organisasi, _ := strconv.ParseUint(c.Param("id_jenis_dokumen_organisasi"), 0, 64)
	result, err := h.arsipDokumenService.FindJenisDokumenByIdJDO(id_jenis_dokumen_organisasi)
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", result)
}

// func (h *arsipDokumenHandler) GetStatusDokumen(c *gin.Context) {
// 	id, _ := strconv.ParseUint(c.Param("id"), 0, 64)
// 	result, err := h.arsipDokumenService.GetStatusDokumen(id)
// 	if err != nil {
// 		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
// 		return
// 	}
// 	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", result)
// }

func (h *arsipDokumenHandler) FindTingkatKerahasiaanByIdJDO(c *gin.Context) {
	id_jenis_dokumen_organisasi, _ := strconv.ParseUint(c.Param("id_jenis_dokumen_organisasi"), 0, 64)
	result, err := h.arsipDokumenService.FindTingkatKerahasiaanByIdJDO(id_jenis_dokumen_organisasi)
	if err != nil {
		util.APIResponse(c, "Data tidak ditemukan", http.StatusBadRequest, "Data tidak ditemukan", nil)
		return
	}
	util.APIResponse(c, "Retrieve data success!", http.StatusOK, "ok", result)
}
