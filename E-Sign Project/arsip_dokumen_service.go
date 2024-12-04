package service

import (
	"bytes"
	"e-sign-backend/entity"
	"e-sign-backend/repository"
	"e-sign-backend/request"
	"e-sign-backend/response"
	"e-sign-backend/util"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"mime/multipart"
	"net/http"
	"os"
	"path/filepath"
	"reflect"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

type ArsipDokumenService interface {
	// Daftar
	GetJenisDokumenDropdown() ([]response.JenisDokumenResponse, error)
	GetStatusDokumenDropdown() ([]entity.StatusDokumen, error)
	GetAllArsipDokumen(nomor_pengajuan string, nama_penandatangan string, nama_pengaju string, jenis_dokumen int64, tanggal string,
		status_dokumen int64, penandatangan_login int64, pengaju_login string, sign_page bool) ([]response.ArsipDokumenResponse, error)
	GetArsipSignList(nomor_surat string, username_pengaju string, id_jenis_dokumen int64, tanggal_pengajuan string, penandatangan_login int64) ([]response.ArsipDokumenResponse, error)
	// GetArsipSignListBatch(nomor_surat string, username_pengaju string, id_jenis_dokumen int64, tanggal_pengajuan string, penandatangan_login int64, batchId int) ([]response.ArsipDokumenResponse, error)
	GetArsipRejectList(username_pengaju string) ([]response.ArsipDokumenResponse, error)
	GetConfirmedOrDeniedRejectArsip(id_penandatangan_login int) ([]response.ArsipDokumenResponse, error)
	GetArsipList(username_pengaju_login string, id_penandatangan_login int,
		nama_penandatangan string, username_pengaju string, nomor_surat string,
		tanggal_pengajuan string, id_jenis_dokumen int, id_status_dokumen int,
		nomor_halaman int, total_data int64) ([]response.ArsipDokumenResponse, int64, error)
	GetDokumen(c *gin.Context, field string, value string, tipe string) error

	// Pengajuan
	GetOrganisasiDropdown() ([]response.OrganisasiResponse, error)
	// GetPemaraf(tipe_ttd int, id_jenis_dokumen_organisasi int) ([]response.PenggunaPemarafTtdResponse, error)
	CreateArsip(ctx *gin.Context, req entity.ArsipDokumen, ms MailService, kode_org string, alurPersetujuan string, namaDokumen string, pdfSource string, imgParameter string) (entity.ArsipDokumen, error)
	GetJenisTtd() ([]entity.JenisTtd, error)
	GetDisclaimerBsre(ctx *gin.Context) (entity.Pengaturan, float64, error)
	GetDropdownPemaraf() ([]response.PenggunaPemarafTtdDropdownResponse, error)
	GenerateNomorPengajuan(kode_org string) (string, error)
	GetOrganisasiByPenggunaId(id uint64) (response.OrganisasiResponse, error)

	//Detail Page dan Tandatangan
	GetListEligibleIdPengguna(id_arsip_dokumen uint64) ([]uint64, error)
	FindArsipById(id uint64) (entity.ArsipDokumen, error)
	GetPenggunaFromArsip(id_arsip_dokumen uint64, tipe int, urutan_ttd int) ([]response.ArsipDokumenResponse, error)
	GetStatusDokumen(id uint64) (entity.StatusDokumen, error)
	// CheckPenandatanganPemaraf(id_pengguna uint64, tipe_ttd int64, id_arsip_dokumen uint64) (entity.Pengguna, error)
	Sign(ctx *gin.Context, req request.SignArsipDokumenRequest, ms MailService) error
	ValidateSignBulk(ctx *gin.Context, req []request.SignArsipDokumenRequest, penggunaService PenggunaService) error
	SignBulk(ctx *gin.Context, req []request.SignArsipDokumenRequest, ms MailService, penggunaService PenggunaService) error
	// SignBatch(ctx *gin.Context, req request.SignBatchArsipRequest, ms MailService, penggunaService PenggunaService) error
	GetFinishedListBulk(id_penandatangan_login int) ([]response.ArsipDokumenResponse, error)
	RemoveNotificationBulk(ctx *gin.Context, req []request.SignArsipDokumenRequest) error

	//Reject (Penolakan) - Confirm Reject - Deny Reject - Cancel (Batalkan)
	RejectArsip(ctx *gin.Context, ms MailService, req request.RejectRequest) error
	ConfirmReject(ctx *gin.Context, ms MailService, req request.PengajuRequest) error
	// DenyReject(ctx *gin.Context, ms MailService, id_ttd_arsip_dokumen uint64) error
	DenyReject(ctx *gin.Context, ms MailService, req request.DenyRejectRequest) error

	CancelArsip(ctx *gin.Context, ms MailService, id uint64) error
	UpdateTime(ctx *gin.Context, id_ttd_arsip_dokumen uint64) error
	SetNonaktif(ctx *gin.Context, req request.NonaktifArsipRequest) error

	//API
	TransformInput(ctx *gin.Context, req request.ExternalArsipDokumenRequest, ms MailService) (entity.ArsipDokumen, error)
	// TransformBatch(ctx *gin.Context, req request.BatchArsipDokumenRequest, ms MailService) (response.BatchResponse, error)
	AturUlangPengajuan(ctx *gin.Context, req request.ExternalArsipDokumenRequest) (response.AturUlangResponse, error)
	// RedirectPengajuan(ctx *gin.Context, id_jenis_dokumen_organisasi int, source string, pengguna_entry string, file_name string)
	DeleteTemporaryFile(file_name string) error
	FindJenisDokumenByIdJDO(id_jenis_dokumen_organisasi uint64) (response.JenisDokumenResponse, error)
	FindOrganisasiByIdJDO(id_jenis_dokumen_organisasi uint64) (response.OrganisasiResponse, error)
	FindTingkatKerahasiaanByIdJDO(id_jenis_dokumen_organisasi uint64) (entity.TingkatKerahasiaan, error)
	GetStatusByIdArsip(id uint64) (response.StatusDokumenResponse, error)
	FindArsipByCodeTransaction(codeTransaction string) (response.StatusDokumenResponse, error)
	FindTempTtdOverrideByCode(code string) ([]entity.TempTtdOverride, error)
	DeleteTempTtdOverrideByCode(code string) error
	// Other
	VerifyFile(ctx *gin.Context) (response.VerifyBSREResponse, error)
	VerifyFileById(req request.VerifyFileRequest) (response.VerifyByDokumenBSREResponse, error)

	//Testing
	TestEmail(ms MailService) error
	// ApiBsreGetUserStatus() (string, string, error)
	// ApiBsreSign(tx *gorm.DB, path_file string, id_user_login uint64, passphrase string) ([]byte, string, error)
	// ApiBsreSignTest(path_file string, id_user_login uint64, passphrase string) ([]byte, string, error)
	// ApiBsreSignWithImage( /*tx *gorm.DB, */ id_arsip uint64, id_ttd_arsip_dokumen uint64, id_user_login uint64) (string, error)
}

type arsipDokumenService struct {
	arsipDokumenRepository        repository.ArsipDokumenRepository
	logService                    LogService
	logEsignService               LogEsignService
	penggunaService               PenggunaService
	jenisDokumenOrganisasiService JenisDokumenOrganisasiService
	DB                            *gorm.DB
	pengaturanService             PengaturanService
	pengaturanEksternalService    PengaturanEksternalService
	batchDokumenRepository        repository.BatchDokumenRepository
}

func NewArsipDokumenService(DB *gorm.DB, arsipDokumenRepository repository.ArsipDokumenRepository, logService LogService, logEsignService LogEsignService, penggunaService PenggunaService, jenisDokumenOrganisasiService JenisDokumenOrganisasiService, pengaturanService PengaturanService, pengaturanEksternalService PengaturanEksternalService, batchDokumenRepository repository.BatchDokumenRepository) *arsipDokumenService {
	return &arsipDokumenService{DB: DB, arsipDokumenRepository: arsipDokumenRepository, logService: logService, logEsignService: logEsignService, penggunaService: penggunaService, jenisDokumenOrganisasiService: jenisDokumenOrganisasiService, pengaturanService: pengaturanService, pengaturanEksternalService: pengaturanEksternalService, batchDokumenRepository: batchDokumenRepository}
}

func (s *arsipDokumenService) GetOrganisasiDropdown() ([]response.OrganisasiResponse, error) {
	result, err := s.arsipDokumenRepository.GetOrganisasiDropdown()
	return result, err
}

func (s *arsipDokumenService) GetJenisDokumenDropdown() ([]response.JenisDokumenResponse, error) {
	result, err := s.arsipDokumenRepository.GetJenisDokumenDropdown()
	return result, err
}

func (s *arsipDokumenService) GetStatusDokumenDropdown() ([]entity.StatusDokumen, error) {
	result, err := s.arsipDokumenRepository.GetStatusDokumenDropdown()
	return result, err
}

func (s *arsipDokumenService) GetAllArsipDokumen(nomor_pengajuan string, nama_penandatangan string, nama_pengaju string, jenis_dokumen int64,
	tanggal string, status_dokumen int64, penandatangan_login int64, pengaju_login string, sign_page bool) ([]response.ArsipDokumenResponse, error) {
	result, err := s.arsipDokumenRepository.GetAllArsipDokumen(nomor_pengajuan, nama_penandatangan, nama_pengaju,
		jenis_dokumen, tanggal, status_dokumen, penandatangan_login, pengaju_login, sign_page)
	return result, err
}

func (s *arsipDokumenService) GetArsipSignList(nomor_surat string, username_pengaju string, id_jenis_dokumen int64, tanggal_pengajuan string, penandatangan_login int64) ([]response.ArsipDokumenResponse, error) {
	result, err := s.arsipDokumenRepository.GetArsipSignList(nomor_surat, username_pengaju, id_jenis_dokumen, tanggal_pengajuan, penandatangan_login)
	return result, err
}

// func (s *arsipDokumenService) GetArsipSignListBatch(nomor_surat string, username_pengaju string, id_jenis_dokumen int64, tanggal_pengajuan string, penandatangan_login int64, batchId int) ([]response.ArsipDokumenResponse, error) {
// 	arsipList, err := s.arsipDokumenRepository.GetArsipSignListBatch(nomor_surat, username_pengaju, id_jenis_dokumen, tanggal_pengajuan, penandatangan_login, batchId)
// 	// Use a map to store unique BatchId values and corresponding ArsipDokumenResponse
// 	uniqueBatchIds := make(map[int]response.ArsipDokumenResponse)
// 	// Only add to the map if BatchId is not already present
// 	for _, i := range arsipList {
// 		// Only add to the map if BatchId is not already present
// 		if _, ok := uniqueBatchIds[i.BatchId]; !ok {
// 			uniqueBatchIds[i.BatchId] = i
// 		}
// 	}
// 	// Extract unique ArsipDokumenResponse values from the map
// 	var result []response.ArsipDokumenResponse
// 	for _, arsip := range uniqueBatchIds {
// 		result = append(result, arsip)
// 	}
// 	return result, err
// }

func (s *arsipDokumenService) GetArsipRejectList(username_pengaju string) ([]response.ArsipDokumenResponse, error) {
	result, err := s.arsipDokumenRepository.GetArsipRejectList(username_pengaju)
	return result, err
}

func (s *arsipDokumenService) GetConfirmedOrDeniedRejectArsip(id_penandatangan_login int) ([]response.ArsipDokumenResponse, error) {
	result, err := s.arsipDokumenRepository.GetConfirmedOrDeniedRejectArsip(id_penandatangan_login)
	return result, err
}

func (s *arsipDokumenService) GetArsipList(username_pengaju_login string,
	id_penandatangan_login int, nama_penandatangan string, username_pengaju string,
	nomor_surat string, tanggal_pengajuan string, id_jenis_dokumen int, id_status_dokumen int,
	nomor_halaman int, totalData int64) ([]response.ArsipDokumenResponse, int64, error) {
	result, err := s.arsipDokumenRepository.GetArsipList(username_pengaju_login, id_penandatangan_login, nama_penandatangan,
		username_pengaju, nomor_surat, tanggal_pengajuan, id_jenis_dokumen, id_status_dokumen, nomor_halaman)

	if totalData == 0 {
		totalData = s.arsipDokumenRepository.CountArsipData(username_pengaju_login,
			id_penandatangan_login, nama_penandatangan,
			username_pengaju, nomor_surat, tanggal_pengajuan,
			id_jenis_dokumen, id_status_dokumen)
	}

	log.Println("Get Arsip Data Result:" + fmt.Sprintf("%v", len(result)) + ", Total Data:" + fmt.Sprintf("%v\n", totalData))
	return result, totalData, err
}

func (s *arsipDokumenService) GetDropdownPemaraf() ([]response.PenggunaPemarafTtdDropdownResponse, error) {
	pemarafDropdown, err := s.arsipDokumenRepository.GetDropdownPemaraf()
	return pemarafDropdown, err
}

func (s *arsipDokumenService) GetJenisTtd() ([]entity.JenisTtd, error) {
	jenisTtd, err := s.arsipDokumenRepository.GetJenisTtd()
	var res []entity.JenisTtd
	for _, i := range jenisTtd {
		if i.Id != 3 {
			res = append(res, entity.JenisTtd{Id: i.Id, Nama: i.Nama, Deskripsi: i.Deskripsi})
		}
	}
	return res, err
	// return jenisTtd, err
}

func (s *arsipDokumenService) GetDokumen(c *gin.Context, field string, value string, tipe string) error {
	// var v interface{}
	var path string
	var err error
	if tipe == "dokumen" {
		var dokumen entity.ArsipDokumen
		switch field {
		case "id":
			id, err := strconv.Atoi(value)
			if err != nil {
				return err
			}
			dokumen, err = s.arsipDokumenRepository.FindArsipById(uint64(id))
			if err != nil {
				return err
			}
		case "nomor_surat":
			dokumen, err = s.arsipDokumenRepository.FindArsipByNomorSurat(value)
			if err != nil {
				return err
			}
		case "id_transaction":
			dokumen, err = s.arsipDokumenRepository.FindArsipByCodeTransaction(value)
			if err != nil {
				return err
			}
		default:
			log.Println("Invalid field")
			return errors.New("Invalid field")
		}
		path = dokumen.PathFile
	} else if tipe == "batch" {
		var batch entity.Batch
		var dokumenBatch entity.ArsipDokumenBatch
		switch field {
		case "id_dokumen":
			id, err := strconv.Atoi(value)
			if err != nil {
				return err
			}
			dokumenBatch, err = s.batchDokumenRepository.FindDokumenById(id)
			if err != nil {
				return err
			}
			path = dokumenBatch.PathFile
		case "nomor_surat_dokumen":
			dokumenBatch, err = s.batchDokumenRepository.FindDokumenByNomorSurat(value)
			if err != nil {
				return err
			}
			path = dokumenBatch.PathFile
		case "batch_name":
			batch, err = s.batchDokumenRepository.FindBatchByBatchName(value)
			if err != nil {
				return err
			}
			path = filepath.Join(util.BASE_PATH, util.DOCS_PATH, batch.BatchName+".zip")
		default:
			log.Println("Invalid field")
			return errors.New("Invalid field")
		}
	} else {
		log.Println("Invalid tipe")
		return errors.New("invalid tipe, tipe hanya bernilai 'dokumen' atau 'batch'")
	}

	err = util.AttachFileToContext(c, filepath.Base(path), path)
	if err != nil {
		return err
	}
	return err
}

func (s *arsipDokumenService) GetDisclaimerBsre(ctx *gin.Context) (entity.Pengaturan, float64, error) {
	var disclaimer entity.Pengaturan
	var fontSize float64
	disclaimer, err := s.pengaturanService.GetPengaturanByField(ctx, "system", "variable", "BSRE_DISCLAIMER")
	if err != nil {
		return disclaimer, fontSize, err
	}
	fontSizeObject, err := s.pengaturanService.GetPengaturanByField(ctx, "system", "variable", "BSRE_DISCLAIMER_FONT_SIZE")
	if err != nil {
		return disclaimer, fontSize, err
	}
	fontSize, err = strconv.ParseFloat(fontSizeObject.Value, 64)
	if err != nil {
		return disclaimer, fontSize, err
	}
	return disclaimer, fontSize, err
}

func (s *arsipDokumenService) AturUlangPengajuan(ctx *gin.Context, req request.ExternalArsipDokumenRequest) (response.AturUlangResponse, error) {
	// url := os.Getenv("APP_URL") + "/ext/submit"
	var result response.AturUlangResponse
	jdo, err := s.jenisDokumenOrganisasiService.FindJenisDokumenOrganisasi(int(req.IdJenisDokumenOrganisasi))
	if err != nil {
		return result, err
	}
	if !jdo.Status {
		return result, errors.New("Konfigurasi Dokumen yang dipilih berstatus tidak aktif")
	}

	jenisDokumen, err := s.arsipDokumenRepository.FindJenisDokumenByIdJDO(req.IdJenisDokumenOrganisasi)
	if err != nil {
		return result, err
	}

	person, _, err := s.penggunaService.FindPenggunaByUsernameComplete(req.PenggunaEntry)
	log.Println("info username pengaju ", person)
	if err != nil {
		return result, err
	}

	//handle upload file
	var file_name string
	_, errFile := ctx.FormFile("file")
	if errFile == nil {
		current_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))

		file_name = "tmp_" + req.PenggunaEntry + "_" + current_time.Format("2006-01-02_15-04-05")
		fmt.Println("file_name: " + file_name)
		file_name_with_ext := file_name + ".pdf"
		target := filepath.Join(util.BASE_PATH, util.DOCS_PATH, file_name_with_ext)
		// if target[0] == '/' || target[0] == '\\' {
		// 	target = target[1:]
		// }
		fmt.Println("saving tmp file to: " + target)
		err = util.SaveFileFromContext(ctx, target)
		if err != nil {
			fmt.Println("error saving tmp file")
			return result, err
		}
		result.Filename = file_name
	}

	//handle override
	if req.TtdArsipDokumen != nil {
		log.Println("override ttd")
		ttdList, _, err := s.OverrideKonfigurasi(req, jenisDokumen.AlurPersetujuan)
		if err != nil {
			fmt.Println("error getting override")
			return result, err
		}
		codeOverride := req.PenggunaEntry + time.Now().Format("20060102150405")
		tx := s.DB.Begin()
		for _, i := range ttdList {
			override := entity.TempTtdOverride{CodeOverride: codeOverride, IdPengguna: &i.IdPengguna, UrutanHirarki: &i.UrutanTtd, TipeTtd: &i.TipeTtd, FlagVisual: i.FlagVisual}
			err = s.arsipDokumenRepository.CreateTempTtdOverride(tx, override)
		}
		tx.Commit()
		result.CodeOverride = codeOverride
	}
	fmt.Println("result", result)

	return result, err
	// url := os.Getenv("APP_URL") + "/ext/submit"
	// url += "?id=" + id + "&source=" + req.SourceApk + "&pengguna_entry=" + req.PenggunaEntry + "&file_name=" + file_name
	// log.Println("url :" + url)
	// ctx.Redirect(http.StatusMovedPermanently, url)
}

func (s *arsipDokumenService) DeleteTempTtdOverrideByCode(code string) error {
	tx := s.DB.Begin()
	err := s.arsipDokumenRepository.DeleteTempTtdOverrideByCode(tx, code)
	if err != nil {
		return err
	}
	tx.Commit()
	return nil
}

func (s *arsipDokumenService) DeleteTemporaryFile(file_name string) error {
	targetPath := filepath.Join(util.BASE_PATH, util.DOCS_PATH, file_name+".pdf")

	err := util.DeleteFileByFilePath(targetPath)
	if err != nil {
		return err
	}
	return nil
}

// func (s *arsipDokumenService) RedirectPengajuan(ctx *gin.Context, id_jenis_dokumen_organisasi int, source string, pengguna_entry string, file_name string) {
// 	url := os.Getenv("APP_URL") + "/ext/submit"
// 	id := strconv.Itoa(id_jenis_dokumen_organisasi)
// 	url += "?id=" + id + "&source=" + source + "&pengguna_entry=" + pengguna_entry + "&file_name=" + file_name
// 	log.Println("url :" + url)
// 	ctx.Redirect(http.StatusMovedPermanently, url)
// }

// func (s *arsipDokumenService) TransformBatch(ctx *gin.Context, req request.BatchArsipDokumenRequest, ms MailService) (response.BatchResponse, error) {
// 	var res response.BatchResponse
// 	var batch entity.Batch
// 	var err error
// 	var emailReq entity.ArsipDokumen
// 	var usernamePengaju string

// 	fmt.Println("source", req.SourceApk)
// 	fmt.Println("IdJDO", req.IdJenisDokumenOrganisasi)

// 	tx := s.DB.Begin()
// 	if len(req.Data) > 0 {
// 		usernamePengaju = req.Data[0].UsernamePengaju
// 		//generate Batch Id
// 		batch, err = s.arsipDokumenRepository.CreateBatch(s.DB, entity.Batch{PenggunaEntry: usernamePengaju})
// 		if err != nil {
// 			tx.Rollback()
// 			return res, err
// 		}
// 		fmt.Println("batch ID: ", batch.ID)
// 	} else {
// 		return res, errors.New("Data pengajuan batch kosong")
// 	}

// 	jdo, err := s.jenisDokumenOrganisasiService.FindJenisDokumenOrganisasi(int(req.IdJenisDokumenOrganisasi))
// 	if err != nil {
// 		return res, err
// 	}
// 	if !jdo.Status {
// 		return res, errors.New("Konfigurasi Dokumen yang dipilih berstatus tidak aktif")
// 	}
// 	jenisDokumen, err := s.arsipDokumenRepository.FindJenisDokumenByIdJDO(req.IdJenisDokumenOrganisasi)
// 	if err != nil {
// 		return res, err
// 	}
// 	organisasi, err := s.arsipDokumenRepository.FindOrganisasiByIdJDO(req.IdJenisDokumenOrganisasi)
// 	if err != nil {
// 		return res, err
// 	}

// 	alurPersetujuan := jenisDokumen.AlurPersetujuan
// 	namaDokumen := jenisDokumen.Nama

// 	//debug
// 	fmt.Println(emailReq, alurPersetujuan, namaDokumen)
// 	for index, i := range req.Data {
// 		// fmt.Println("FileBase64", strings.TrimSpace(i.FileBase64[0:75]))

// 		fmt.Println("username pengaju", i.UsernamePengaju)
// 		fmt.Println("no surat", i.NomorSurat)
// 		_, _, err := s.penggunaService.FindPenggunaByUsernameComplete(i.UsernamePengaju)
// 		if err != nil {
// 			return res, err
// 		}

// 		ext := request.ExternalArsipDokumenRequest{PenggunaEntry: i.UsernamePengaju}
// 		ttdList, jenisList, err := s.GetKonfigurasiFromJDO(jdo, ext, jenisDokumen.AlurPersetujuan)
// 		if err != nil {
// 			return res, err
// 		}

// 		input := entity.ArsipDokumen{
// 			IdJenisDokumenOrganisasi: req.IdJenisDokumenOrganisasi,
// 			PenggunaEntry:            i.UsernamePengaju,
// 			NomorSurat:               i.NomorSurat,
// 			SourceApk:                req.SourceApk,
// 			TtdArsipDokumen:          ttdList,
// 			JenisTtdArsipDokumen:     jenisList,
// 			BatchId:                  sql.NullInt64{int64(batch.ID), true},
// 		}
// 		if index == 0 {
// 			emailReq = input
// 		}

// 		conv, err := s.ConvertToBase64(nil, input, true, i.FileBase64)
// 		if err != nil {
// 			log.Println(err)
// 			return res, errors.New("convert error")
// 		}
// 		imgParameterJSON, err := json.Marshal(conv.ImgParameter)
// 		if err != nil {
// 			fmt.Println("convert img error")
// 			return res, errors.New("convert img error")
// 		}
// 		imgParameterString := string(imgParameterJSON)

// 		err = s.CreateBatch(ctx, input, ms, organisasi.KodeOrg, jenisDokumen, conv, imgParameterString)
// 		if err != nil {
// 			tx.Rollback()
// 			return res, err
// 		}
// 	}
// 	err = s.SendBatchEmail(emailReq, ms, alurPersetujuan, namaDokumen, strconv.Itoa(int(batch.ID)))
// 	if err != nil {
// 		return res, err
// 	}
// 	res.BatchId = batch.ID
// 	res.PenggunaEntry = usernamePengaju
// 	res.JumlahDokumen = len(req.Data)
// 	tx.Commit()
// 	return res, err
// 	// batchId, err := password.Generate(5, 2, 0, false, false)
// 	// if err != nil {
// 	// 	log.Println(err)
// 	// }
// 	// batchFound, err := s.arsipDokumenRepository.FindArsipByBatchId(batchId)
// 	// for batchFound != "" {
// 	// 	batchId, err = password.Generate(5, 2, 0, false, false)
// 	// 	if err != nil {
// 	// 		log.Println(err)
// 	// 	}
// 	// 	batchFound, err = s.arsipDokumenRepository.FindArsipByBatchId(batchId)
// 	// }
// }

func (s *arsipDokumenService) TransformInput(ctx *gin.Context, req request.ExternalArsipDokumenRequest, ms MailService) (entity.ArsipDokumen, error) {
	// req := external.Detail
	person, _, err := s.penggunaService.FindPenggunaByUsernameComplete(req.PenggunaEntry)
	log.Println("info username pengaju " + person.Username)
	if err != nil {
		return entity.ArsipDokumen{}, err
	}

	jdo, err := s.jenisDokumenOrganisasiService.FindJenisDokumenOrganisasi(int(req.IdJenisDokumenOrganisasi))
	if err != nil {
		return entity.ArsipDokumen{}, err
	}
	if !jdo.Status {
		return entity.ArsipDokumen{}, errors.New("Konfigurasi Dokumen yang dipilih berstatus tidak aktif")
	}

	jenisDokumen, err := s.arsipDokumenRepository.FindJenisDokumenByIdJDO(req.IdJenisDokumenOrganisasi)
	if err != nil {
		return entity.ArsipDokumen{}, err
	}
	organisasi, err := s.arsipDokumenRepository.FindOrganisasiByIdJDO(req.IdJenisDokumenOrganisasi)
	if err != nil {
		return entity.ArsipDokumen{}, err
	}
	log.Println("organisasi :" + fmt.Sprintf("%v\n", organisasi))
	log.Println("jenisDokumen :" + fmt.Sprintf("%v\n", jenisDokumen))

	ttdList := []entity.TtdArsipDokumen{}
	jenisList := []entity.JenisTtdArsipDokumen{}

	if req.TtdArsipDokumen != nil {
		log.Println("override ttd")
		ttdList, jenisList, err = s.OverrideKonfigurasi(req, jenisDokumen.AlurPersetujuan)
	} else {
		log.Println("copy from template")
		ttdList, jenisList, err = s.GetKonfigurasiFromJDO(jdo, req, jenisDokumen.AlurPersetujuan)
	}
	if err != nil {
		return entity.ArsipDokumen{}, err
	}

	input := entity.ArsipDokumen{
		IdJenisDokumenOrganisasi: req.IdJenisDokumenOrganisasi,
		PenggunaEntry:            req.PenggunaEntry,
		NomorSurat:               req.NomorSurat,
		SourceApk:                req.SourceApk,
		TtdArsipDokumen:          ttdList,
		JenisTtdArsipDokumen:     jenisList,
	}
	conv, err := s.ConvertToBase64(ctx, input, false, "")
	if err != nil {
		log.Println(err)
		return entity.ArsipDokumen{}, errors.New("convert error")
	}
	// for _, i := range input.TtdArsipDokumen {
	// 	log.Println("id penguna", i.IdPengguna)
	// 	fmt.Printf("Flag %t", i.FlagUrutan)
	// }
	inputJSON, err := json.Marshal(input)
	if err != nil {
		return entity.ArsipDokumen{}, err
	}

	imgParameterJSON, err := json.Marshal(conv.ImgParameter)
	if err != nil {
		fmt.Println("convert img error")
		return entity.ArsipDokumen{}, errors.New("convert img error")
	}
	// fmt.Println("imgParameterJSON", string(imgParameterJSON))
	imgParameterString := string(imgParameterJSON)
	// if imgParameterJSON == nil {
	// 	imgParameterString = ""
	// }

	// if imgParameterString == "[]" {
	// 	imgParameterString = ""
	// }
	// fmt.Println("imgParameterString", imgParameterString)
	// log.Println("imgParameterJSON ", string(imgParameterJSON))
	log.Println("input " + string(inputJSON))

	result, err := s.CreateArsip(ctx, input, ms, organisasi.KodeOrg, jenisDokumen.AlurPersetujuan, jenisDokumen.Nama, conv.PdfBase64, imgParameterString)
	if err != nil {
		return entity.ArsipDokumen{}, err
	}
	return result, err
}

func (s *arsipDokumenService) GetKonfigurasiFromJDO(jdo entity.JenisDokumenOrganisasi, req request.ExternalArsipDokumenRequest, alurPersetujuan string) ([]entity.TtdArsipDokumen, []entity.JenisTtdArsipDokumen, error) {
	ttdList := []entity.TtdArsipDokumen{}
	jenisList := []entity.JenisTtdArsipDokumen{}

	for _, i := range jdo.TtdJenisDokumen {
		if i.IdJenisTtd != nil {
			if *i.IdJenisTtd > uint64(0) {
				jenisList = append(jenisList, entity.JenisTtdArsipDokumen{
					IdJenisTtd: *i.IdJenisTtd,
					Koordinat:  i.Koordinat})
			}
		}
		if i.IdPengguna != nil {
			if *i.IdPengguna > uint64(0) {
				// false_flag := false
				person, _, _, err := s.penggunaService.FindPenggunaByIdComplete(*i.IdPengguna)
				if err != nil {
					return ttdList, jenisList, err
				}
				ttdList = append(ttdList, entity.TtdArsipDokumen{
					IdPengguna:    *i.IdPengguna,
					PenggunaEntry: req.PenggunaEntry,
					Koordinat:     i.Koordinat,
					UrutanTtd:     *i.UrutanHirarki,
					TipeTtd:       *i.TipeTtd,
					FlagTtd:       false,
					// FlagUrutan:    &false_flag,
					FlagVisual: i.FlagVisual,
					Jabatan:    person.Jabatan})
			}
		}
	}

	// requestTtd := req.TtdArsipDokumen
	log.Println("before sort :" + fmt.Sprintf("%v\n", ttdList))
	sort.Slice(ttdList, func(i, j int) bool {
		if ttdList[i].TipeTtd != ttdList[j].TipeTtd {
			return ttdList[i].TipeTtd < ttdList[j].TipeTtd
		}
		return ttdList[i].UrutanTtd < ttdList[j].UrutanTtd
	})

	log.Println("after sort :" + fmt.Sprintf("%v\n", ttdList))
	for i := range ttdList {
		true_flag := true
		false_flag := false
		if alurPersetujuan == "Langsung" || alurPersetujuan == "Grup" {
			ttdList[i].FlagUrutan = &true_flag
			// i.FlagUrutan = &true_flag
		} else {
			if i == 0 {
				ttdList[i].FlagUrutan = &true_flag
			} else {
				ttdList[i].FlagUrutan = &false_flag
			}
		}
	}
	return ttdList, jenisList, nil
}

func (s *arsipDokumenService) OverrideKonfigurasi(req request.ExternalArsipDokumenRequest, alurPersetujuan string) ([]entity.TtdArsipDokumen, []entity.JenisTtdArsipDokumen, error) {
	ttdList := []entity.TtdArsipDokumen{}
	jenisList := []entity.JenisTtdArsipDokumen{}
	requestTtds := req.TtdArsipDokumen
	requestJenis := req.JenisTtdArsipDokumen

	penandatanganCount := 0
	for _, t := range requestTtds {
		log.Println("value :" + fmt.Sprintf("%v\n", t))
		if t.TipeTtd == 2 {
			penandatanganCount += 1
		}
	}

	if penandatanganCount <= 0 {
		return ttdList, jenisList, errors.New("Daftar penandatangan kosong. Harap isi terlebih dahulu")
	}

	if alurPersetujuan == "Langsung" {
		if penandatanganCount > 1 {
			return ttdList, jenisList, errors.New("Workflow Persetujuan Langsung hanya bisa memilih 1 penandatangan")
		}
	}
	if alurPersetujuan == "Grup" || alurPersetujuan == "Berjenjang" {
		if penandatanganCount <= 1 {
			return ttdList, jenisList, errors.New("Workflow Persetujuan Berjenjang atau Grup harus lebih dari 1 penandatangan")
		}
	}

	sort.Slice(requestTtds, func(i, j int) bool {
		if requestTtds[i].TipeTtd != requestTtds[j].TipeTtd {
			return requestTtds[i].TipeTtd < requestTtds[j].TipeTtd
		}
		return requestTtds[i].UrutanTtd < requestTtds[j].UrutanTtd
	})

	for index, t := range requestTtds {
		person, _, err := s.penggunaService.FindPenggunaByUsernameComplete(t.UsernamePenandatangan)
		if err != nil {
			return ttdList, jenisList, err
		}
		true_flag := true
		false_flag := false
		if alurPersetujuan == "Langsung" || alurPersetujuan == "Grup" {
			t.FlagUrutan = &true_flag
		} else {
			if index == 0 {
				t.FlagUrutan = &true_flag
			} else {
				t.FlagUrutan = &false_flag
			}
		}

		ttdList = append(ttdList, entity.TtdArsipDokumen{
			IdPengguna:    person.Id,
			PenggunaEntry: req.PenggunaEntry,
			Koordinat:     t.Koordinat,
			Halaman:       t.Halaman,
			UrutanTtd:     t.UrutanTtd,
			TipeTtd:       t.TipeTtd,
			FlagTtd:       false,
			FlagUrutan:    t.FlagUrutan,
			FlagVisual:    t.FlagVisual,
			Jabatan:       person.Jabatan})
	}

	for _, j := range requestJenis {
		// JenisTtd pake nama, jadi 'stamp', 'qr', atau 'deskripsi qr'
		jenisTtd, err := s.arsipDokumenRepository.FindJenisTtdByNama(j.JenisTtd)
		if err != nil {
			return ttdList, jenisList, err
		}
		jenisList = append(jenisList, entity.JenisTtdArsipDokumen{
			IdJenisTtd: jenisTtd.Id,
			Koordinat:  j.Koordinat,
			Halaman:    j.Halaman})
	}
	return ttdList, jenisList, nil
}

func (s *arsipDokumenService) CreateBatch(ctx *gin.Context, req entity.ArsipDokumen, ms MailService, kode_org string,
	jenisDokumen response.JenisDokumenResponse, conv request.ConverterPdfRequest, imgParameter string) error {
	tx := s.DB.Begin()
	var err error
	req.FileDokumen = ""
	req.PathFile = ""
	nomor_pengajuan, err := s.GenerateNomorPengajuan(kode_org)
	if err != nil {
		return err
	}
	req.NomorPengajuan = nomor_pengajuan

	input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	req.TanggalEntry = &input_time
	req.IdStatusDokumen = uint64(1)
	// log.Println("req after: ")
	// log.Println(fmt.Printf("%+v", req))
	req, err = s.arsipDokumenRepository.CreateArsip(tx, req)
	if err != nil {
		tx.Rollback()
		return err
	}
	// writing to log, pengajuan ttd insert to arsip_dokumen
	if err = s.logService.WriteLogAktivitas(ctx, req.PenggunaEntry, "Pengaju", "Pengajuan ttd: insert to arsip_dokumen and ttd_arsip_dokumen",
		"", req.ToString(), req.TableName()); err != nil {
		tx.Rollback()
		return err
	}

	var bytes []byte
	if conv.PdfBase64 != "" {
		bytes, err = base64.StdEncoding.DecodeString(conv.PdfBase64)
		if err != nil {
			return err
		}
	} else {
		return errors.New("base64 empty for nomor surat:" + req.NomorSurat)
	}
	arsip := req
	for _, ttd := range req.TtdArsipDokumen {
		input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
		ttd.TanggalEntry = &input_time
		// err = s.arsipDokumenRepository.UpdateTtdArsip(tx, ttd)
		err = s.arsipDokumenRepository.UpdateTtdArsip(tx, ttd)
		if err != nil {
			tx.Rollback()
			return err
		}
	}
	if err = s.logService.WriteLogAktivitas(ctx, req.PenggunaEntry, "Pengaju", "Pengajuan ttd: update tanggal_entry of ttd_arsip_dokumen",
		arsip.ToString(), req.ToString(), req.TtdArsipDokumen[0].TableName()); err != nil {
		tx.Rollback()
		return err
	}

	filename := strconv.FormatUint(req.Id, 10) + ".pdf"
	targetPath := filepath.Join(util.BASE_PATH, util.DOCS_PATH, filename)

	targetPathRaw := filepath.Join(util.BASE_PATH, util.DOCS_PATH, "raw_"+filename)

	fmt.Println("saving raw file to: " + targetPathRaw)
	err = os.WriteFile(targetPathRaw, bytes, 0644)
	if err != nil {
		tx.Rollback()
		return err
	}
	//Get Disclaimer Text at footer each page
	disclaimer, fontSize, err := s.GetDisclaimerBsre(ctx)
	if err != nil {
		tx.Rollback()
		return err
	}
	err = util.GenerateFinalDocument(ctx, conv.PdfBase64, imgParameter,
		os.Getenv("BASEURL_FRONTEND")+"/dokumen/detail/"+strconv.FormatUint(req.Id, 10), targetPath, req.SourceApk, req.PenggunaEntry, disclaimer.Value, fontSize)
	if err != nil {
		tx.Rollback()
		return err
	}
	req.FileDokumenSigned = targetPath
	req.FileDokumen = filename
	req.PathFile = targetPath
	err = s.arsipDokumenRepository.UpdateArsip(tx, req)
	if err != nil {
		tx.Rollback()
		return err
	}
	if err = s.logService.WriteLogAktivitas(ctx, req.PenggunaEntry, "Pengaju", "Pengajuan ttd: update file and path_file of arsip_dokumen",
		arsip.ToString(), req.ToString(), req.TableName()); err != nil {
		tx.Rollback()
		return err
	}
	tx.Commit()
	return err
}

func (s *arsipDokumenService) CreateArsip(ctx *gin.Context, req entity.ArsipDokumen, ms MailService, kode_org string,
	alurPersetujuan string, namaDokumen string, pdfSource string, imgParameter string) (entity.ArsipDokumen, error) {
	isDuplicateNomorSurat := false
	exist, _ := s.arsipDokumenRepository.FindArsipByNomorSurat(req.NomorSurat)
	if exist.NomorSurat == req.NomorSurat {
		isDuplicateNomorSurat = true
	}
	if isDuplicateNomorSurat {
		return entity.ArsipDokumen{}, errors.New("Nomor surat sudah pernah diajukan.")
	}
	tx := s.DB.Begin()
	fmt.Println("get all request")
	body, _ := io.ReadAll(ctx.Request.Body)
	fmt.Println(string(body))
	req.FileDokumen = ""
	req.PathFile = ""
	// req.BatchId = sql.NullInt64{int64(0), false}

	if req.CodeTransaction != "" {
		existing, err := s.arsipDokumenRepository.FindArsipByCodeTransaction(req.CodeTransaction)
		if errors.Is(err, gorm.ErrRecordNotFound) {
			fmt.Println("Pengajuan dengan Kode Transaksi " + req.CodeTransaction + " tidak ditemukan, pengajuan dapat diproses")
		} else if err != nil {
			tx.Rollback()
			return req, err
		} else {
			tx.Rollback()
			return req, errors.New("Pengajuan dengan Kode Transaksi " + req.CodeTransaction + " sudah ada dengan id pengajuan " + fmt.Sprint(existing.Id))
		}
	}

	var err error
	nomor_pengajuan, err := s.GenerateNomorPengajuan(kode_org)
	if err != nil {
		return req, err
	}
	req.NomorPengajuan = nomor_pengajuan
	header, err := ctx.FormFile("file")
	fileAda := true

	switch err {
	case nil:

	case http.ErrMissingFile:
		fmt.Println("masuk")
		fileAda = false
	default:
		fmt.Println("error file")
		tx.Rollback()
		return req, err
	}

	if req.Id <= 0 {
		input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
		req.TanggalEntry = &input_time
		req.IdStatusDokumen = uint64(1)
		true_flag := true
		req.FlagAktif = &true_flag
		log.Println("req after: ")
		log.Println(fmt.Printf("%+v", req))
		req, err = s.arsipDokumenRepository.CreateArsip(tx, req)
		if err != nil {
			tx.Rollback()
			return req, err
		}
		// writing to log, pengajuan ttd insert to arsip_dokumen
		if err = s.logService.WriteLogAktivitas(ctx, req.PenggunaEntry, "Pengaju", "Pengajuan ttd: insert to arsip_dokumen and ttd_arsip_dokumen",
			"", req.ToString(), req.TableName()); err != nil {
			tx.Rollback()
			return req, err
		}
	}

	// else {
	// 	req.TanggalUpdate.Time = time.Now()
	// 	req.TanggalUpdate.Valid = true
	// 	err = s.jenisDokumenOrganisasiRepositoryImpl.DeleteTtdByJenisDokumenOrganisasi(tx, int(data.Id))
	// 	if err != nil {
	// 		tx.Rollback()
	// 		return data, err
	// 	}
	// 	err = s.jenisDokumenOrganisasiRepositoryImpl.UpdateJenisDokumenOrganisasi(tx, data)
	// 	if err != nil {
	// 		tx.Rollback()
	// 		return data, err
	// 	}
	// 	if fileAda {
	// 		err = s.DeleteFileTtdJenisDokumen(int(data.Id))
	// 		if err != nil {
	// 			tx.Rollback()
	// 			return data, err
	// 		}
	// 	}
	// }
	// tx = s.DB.Begin()

	// writing to log, pengajuan ttd update tanggal_entry of ttd_arsip_dokumen
	arsip := entity.ArsipDokumen{}
	arsip = req
	for _, ttd := range req.TtdArsipDokumen {
		input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
		ttd.TanggalEntry = &input_time
		// err = s.arsipDokumenRepository.UpdateTtdArsip(tx, ttd)
		err = s.arsipDokumenRepository.UpdateTtdArsip(tx, ttd)
		if err != nil {
			tx.Rollback()
			return req, err
		}
	}
	if err = s.logService.WriteLogAktivitas(ctx, req.PenggunaEntry, "Pengaju", "Pengajuan ttd: update tanggal_entry of ttd_arsip_dokumen",
		arsip.ToString(), req.ToString(), req.TtdArsipDokumen[0].TableName()); err != nil {
		tx.Rollback()
		return req, err
	}

	if fileAda {
		// save raw file to esign-be
		filename := strconv.FormatUint(req.Id, 10) + "_" + strings.ReplaceAll(header.Filename, " ", "_")
		targetPath := filepath.Join(util.BASE_PATH, util.DOCS_PATH, filename)

		// fmt.Println("saving file to target: " + targetPath)
		// err = util.SaveFileFromContext(ctx, targetPath)
		// if err != nil {
		// 	tx.Rollback()
		// 	return req, err
		// }
		targetPathRaw := filepath.Join(util.BASE_PATH, util.DOCS_PATH, "raw_"+filename)

		fmt.Println("saving raw file to: " + targetPathRaw)
		err = util.SaveFileFromContext(ctx, targetPathRaw)
		if err != nil {
			tx.Rollback()
			return req, err
		}

		// fmt.Println("imgparameter to converter API: |" + imgParameter + "|")
		// if imgParameter == "[]" {
		// 	imgParameter = ""
		// }
		// if imgParameter != "" {

		// fmt.Println("imgParameter")
		// fmt.Println(imgParameter)
		if imgParameter == "null" {
			imgParameter = "[]"
		}
		//Get Disclaimer Text at footer each page
		disclaimer, fontSize, err := s.GetDisclaimerBsre(ctx)
		if err != nil {
			return req, err
		}
		// fmt.Println(imgParameter)
		err = util.GenerateFinalDocument(ctx, pdfSource, imgParameter,
			os.Getenv("BASEURL_FRONTEND")+"/verify-file?tipe=dokumen&id_arsip_dokumen="+strconv.FormatUint(req.Id, 10), targetPath, req.SourceApk, req.PenggunaEntry, disclaimer.Value, fontSize)
		if err != nil {
			tx.Rollback()
			return req, err
		}
		// }

		req.FileDokumenSigned = targetPath

		// if strings.ToLower(req.SourceApk) == "internal" {
		//get file from converter and save stamped file to esign-be
		// converterURL := os.Getenv("API_URL_CONVERTER") + "/download-surat"
		// converterURL += req.FileDokumenSigned
		// fmt.Println("download file from " + converterURL)
		// reqConv, err := http.NewRequest("GET", converterURL, nil)
		// if err != nil {
		// 	tx.Rollback()
		// 	return req, err
		// }
		// clientConv := http.Client{Timeout: 3 * time.Minute}
		// resConv, err := clientConv.Do(reqConv)
		// if err != nil {
		// 	tx.Rollback()
		// 	return req, err
		// }
		// defer resConv.Body.Close()
		// fmt.Println("status response download from converter: " + resConv.Status)
		// bodyConv, err := io.ReadAll(resConv.Body)
		// if err != nil {
		// 	tx.Rollback()
		// 	fmt.Println(err)
		// }
		// fmt.Println("saving stamped file to " + targetPath)
		// err = os.WriteFile(targetPath, bodyConv, 0644)
		// if err != nil {
		// 	tx.Rollback()
		// 	return req, err
		// }
		//end
		// }

		//send email
		req, err = s.SendEmail(tx, req, ms, alurPersetujuan, namaDokumen)
		if err != nil {
			tx.Rollback()
			return req, err
		}
		//update to db
		arsip = req
		req.FileDokumen = filename
		req.PathFile = targetPath
		log.Println("req after set flag: ")
		log.Println(fmt.Printf("%+v", req))
		// err = s.arsipDokumenRepository.UpdateArsip(tx, req)
		err = s.arsipDokumenRepository.UpdateArsip(tx, req)
		if err != nil {
			tx.Rollback()
			return req, err
		}
		// writing to log, pengajuan ttd update tanggal_entry of ttd_arsip_dokumen
		if err = s.logService.WriteLogAktivitas(ctx, req.PenggunaEntry, "Pengaju", "Pengajuan ttd: update file and path_file of arsip_dokumen",
			arsip.ToString(), req.ToString(), req.TableName()); err != nil {
			tx.Rollback()
			return req, err
		}
	} else {
		tx.Rollback()
		return req, err
	}
	tx.Commit()
	return req, err
}

func (s *arsipDokumenService) ConvertToBase64(ctx *gin.Context, req entity.ArsipDokumen, isBatch bool, pdfbase64 string) (request.ConverterPdfRequest, error) {
	var result request.ConverterPdfRequest
	var imgParameterList []request.ImgParameter
	// var imgParameterList []string

	organisasi, err := s.arsipDokumenRepository.FindOrganisasiByIdJDO(req.IdJenisDokumenOrganisasi)
	if err != nil {
		return result, err
	}

	var base64_file string
	if isBatch {
		base64_file = pdfbase64
	} else {
		file, _, err := ctx.Request.FormFile("file")
		if err != nil {
			return result, err
		}
		defer file.Close()
		buf := bytes.NewBuffer(nil)
		if _, err := io.Copy(buf, file); err != nil {
			return result, err
		}
		base64_file = base64.StdEncoding.EncodeToString(buf.Bytes())
	}

	if strings.ToLower(req.SourceApk) != "internal" {
		var param request.ImgParameter
		fmt.Println("||EXTERNAL|| converting ttd arsip from request to base 64")
		for _, i := range req.TtdArsipDokumen {
			base64Src := ""
			usernamePenandatangan := ""
			if i.IdPengguna > uint64(0) && i.FlagVisual {
				fmt.Println("converting ttd request for id pengguna: ", i.IdPengguna)
				person, _, _, err := s.penggunaService.FindPenggunaByIdComplete(i.IdPengguna)
				if err != nil {
					return result, err
				}
				if i.TipeTtd == uint64(1) {
					base64Src, err = util.GetFiletoBase64(util.BASE_PATH + util.PARAFIMG_PATH + "/" + person.FileParaf)
					if err != nil {
						return result, errors.New("File " + person.FileParaf + " tidak dapat dikonversi ke base64")
					}
					usernamePenandatangan = person.Username + "_paraf"
				} else if i.TipeTtd == uint64(2) {
					base64Src, err = util.GetFiletoBase64(util.BASE_PATH + util.SIGNIMG_PATH + "/" + person.FileTtd)
					usernamePenandatangan = person.Username + "_ttd"
					if err != nil {
						return result, errors.New("File " + person.FileTtd + " tidak dapat dikonversi ke base64")
					}

				}
				if err != nil {
					return result, err
				}

				if i.Koordinat != "" {
					if err := json.Unmarshal([]byte(i.Koordinat), &param); err != nil {
						fmt.Println("error converting koordinat ttd request: ", i.Koordinat)
						return result, err
					}
				} else {
					return result, errors.New("Koordinat is empty")
				}

				imgParameterList = append(imgParameterList, request.ImgParameter{
					Id:        usernamePenandatangan,
					X:         param.X,
					Y:         param.Y,
					Width:     param.Width,
					Height:    param.Height,
					PdfWidth:  param.PdfWidth,
					PdfHeight: param.PdfHeight,
					PdfX:      param.PdfX,
					PdfY:      param.PdfY,
					Page:      param.Page,
					Base64Src: base64Src})
			}
		}

		for _, i := range req.JenisTtdArsipDokumen {
			fmt.Println("converting jenisttd request for id jenis ttd: ", i.IdJenisTtd)
			base64Src := ""
			IdImage := ""
			if i.Koordinat != "" {
				if err := json.Unmarshal([]byte(i.Koordinat), &param); err != nil {
					fmt.Println("error converting koordinat jenisttd request: ", i.Koordinat)
					return result, err
				}
			} else {
				log.Println("Koordinat in JenisTtdArsipDokumen for arsip id ", i.IdArsipDokumen, " halaman ", i.Halaman, " is empty")
				return result, errors.New("Koordinat is empty")
			}

			if i.IdJenisTtd > uint64(0) {
				if i.IdJenisTtd == uint64(1) {
					IdImage = strings.ReplaceAll(organisasi.Nama, " ", "") + "_organisasi"
					base64Src, err = util.GetFiletoBase64(util.BASE_PATH + util.STAMP_PATH + "/" + organisasi.FileStamp)
				} else if i.IdJenisTtd == uint64(2) {
					IdImage = "QRCode"
					base64Src = os.Getenv("APP_URL") + "/verify-file?tipe=dokumen&id_arsip_dokumen=" + strconv.FormatUint(req.Id, 10)
				} else if i.IdJenisTtd == uint64(3) {
					IdImage = "DeskripsiQR"
					base64Src = os.Getenv("BSRE_DISCLAIMER")
				}
			}
			imgParameterList = append(imgParameterList, request.ImgParameter{
				Id:        IdImage,
				X:         param.X,
				Y:         param.Y,
				Width:     param.Width,
				Height:    param.Height,
				PdfWidth:  param.PdfWidth,
				PdfHeight: param.PdfHeight,
				PdfX:      param.PdfX,
				PdfY:      param.PdfY,
				Page:      param.Page,
				Base64Src: base64Src})
		}
		fmt.Println("imgParameterList length: ", len(imgParameterList))
		// if len(imgParameterList) > 0 {
		result.PdfBase64 = base64_file
		result.Username = req.PenggunaEntry
		result.ImgParameter = imgParameterList
		// }
	} else {
		return result, errors.New("Not external request")
	}
	return result, err
}

func (s *arsipDokumenService) GetListEligibleIdPengguna(id_arsip_dokumen uint64) ([]uint64, error) {
	result := []uint64{}
	arsip, err := s.arsipDokumenRepository.FindArsipById(id_arsip_dokumen)
	if err != nil {
		return result, err
	}
	person, err := s.penggunaService.FindPenggunaByUsername(arsip.PenggunaEntry)
	if err != nil {
		return result, err
	}
	result = append(result, person.Id)
	for _, i := range arsip.TtdArsipDokumen {
		result = append(result, i.IdPengguna)
	}
	return result, err
}

func (s *arsipDokumenService) FindArsipById(id uint64) (entity.ArsipDokumen, error) {
	result, err := s.arsipDokumenRepository.FindArsipById(id)
	return result, err
}

func (s *arsipDokumenService) FindTempTtdOverrideByCode(code string) ([]entity.TempTtdOverride, error) {
	result, err := s.arsipDokumenRepository.FindTempTtdOverrideByCode(code)
	return result, err
}

func (s *arsipDokumenService) TestEmail(ms MailService) error {
	p := response.ArsipDokumenResponse{IdPenandatangan: 1, NamaPenandatangan: "John", EmailPenandatangan: "rizalbahriawan@gmail.com"}
	// var m MailService
	err := ms.SignerMail(p, "Ijazah", "admin",
		"/dokumen/detail/"+strconv.FormatUint(p.IdArsip, 10))
	log.Println("email sent to: " + fmt.Sprintf("%+v", p))
	return err
}

func (s *arsipDokumenService) GetPenggunaFromArsip(id_arsip_dokumen uint64, tipe int, urutan_ttd int) ([]response.ArsipDokumenResponse, error) {
	// var signer entity.Pengguna
	signer, err := s.arsipDokumenRepository.GetPenggunaFromArsip(id_arsip_dokumen, tipe, urutan_ttd)
	return signer, err
}

func (s *arsipDokumenService) FindJenisDokumenByIdJDO(id_jenis_dokumen_organisasi uint64) (response.JenisDokumenResponse, error) {
	result, err := s.arsipDokumenRepository.FindJenisDokumenByIdJDO(id_jenis_dokumen_organisasi)
	return result, err
}

func (s *arsipDokumenService) FindOrganisasiByIdJDO(id_jenis_dokumen_organisasi uint64) (response.OrganisasiResponse, error) {
	result, err := s.arsipDokumenRepository.FindOrganisasiByIdJDO(id_jenis_dokumen_organisasi)
	return result, err
}

func (s *arsipDokumenService) FindTingkatKerahasiaanByIdJDO(id_jenis_dokumen_organisasi uint64) (entity.TingkatKerahasiaan, error) {
	result, err := s.arsipDokumenRepository.FindTingkatKerahasiaanByIdJDO(id_jenis_dokumen_organisasi)
	return result, err
}

func (s *arsipDokumenService) GetStatusData(arsip entity.ArsipDokumen) (response.StatusDokumenResponse, error) {
	jenisList := []response.JenisTtdArsipDokumenResponse{}
	signedBy := []response.TtdArsipDokumenResponse{}
	notYetSignedBy := []response.TtdArsipDokumenResponse{}
	var result response.StatusDokumenResponse

	jenisDokumen, err := s.arsipDokumenRepository.FindJenisDokumenByIdJDO(arsip.IdJenisDokumenOrganisasi)
	if err != nil {
		return result, err
	}
	status, err := s.arsipDokumenRepository.GetStatusDokumen(arsip.IdStatusDokumen)
	if err != nil {
		return result, err
	}
	for _, i := range arsip.TtdArsipDokumen {
		tipeTtd := "Paraf"
		if i.TipeTtd == 2 {
			tipeTtd = "Tanda Tangan"
		}

		var alasanPenolakan string
		if i.Keterangan != nil {
			alasanPenolakan = *i.Keterangan
		}
		if i.FlagTtd {
			signedBy = append(signedBy, response.TtdArsipDokumenResponse{
				IdArsipDokumen:  arsip.Id,
				NamaLengkap:     i.Pengguna.NamaLengkap,
				UrutanTtd:       i.UrutanTtd,
				TipeTtd:         tipeTtd,
				FlagTtd:         i.FlagTtd,
				TanggalEntry:    i.TanggalEntry,
				TanggalUpdate:   i.TanggalUpdate,
				FlagUrutan:      i.FlagUrutan,
				AlasanPenolakan: alasanPenolakan,
				FlagVisual:      i.FlagVisual,
				Jabatan:         i.Jabatan})
		} else {
			notYetSignedBy = append(notYetSignedBy, response.TtdArsipDokumenResponse{
				IdArsipDokumen:  arsip.Id,
				NamaLengkap:     i.Pengguna.NamaLengkap,
				UrutanTtd:       i.UrutanTtd,
				TipeTtd:         tipeTtd,
				FlagTtd:         i.FlagTtd,
				TanggalEntry:    i.TanggalEntry,
				TanggalUpdate:   i.TanggalUpdate,
				FlagUrutan:      i.FlagUrutan,
				AlasanPenolakan: alasanPenolakan,
				FlagVisual:      i.FlagVisual,
				Jabatan:         i.Jabatan})
		}
	}
	for _, i := range arsip.JenisTtdArsipDokumen {
		jenisTtd, err := s.arsipDokumenRepository.FindJenisTtdById(i.IdJenisTtd)
		if err != nil {
			return response.StatusDokumenResponse{}, err
		}
		jenisList = append(jenisList, response.JenisTtdArsipDokumenResponse{
			JenisTtd: jenisTtd.Nama})
	}
	result.IdArsipDokumen = arsip.Id
	result.NomorSurat = arsip.NomorSurat
	result.Status = status.Nama
	result.UsernamePengaju = arsip.PenggunaEntry
	result.Source = arsip.SourceApk
	result.CodeTransaction = arsip.CodeTransaction
	result.AlurPersetujuan = jenisDokumen.AlurPersetujuan
	result.FileDokumen = arsip.FileDokumen
	result.FlagAktif = arsip.FlagAktif
	result.KeteranganFlagAktif = arsip.KeteranganFlagAktif
	result.SignedBy = signedBy
	result.Signed = len(signedBy)
	result.NotYetSignedBy = notYetSignedBy
	result.NotYetSigned = len(notYetSignedBy)
	if arsip.TanggalEntry != nil {
		result.LastUpdate = arsip.TanggalEntry
	}
	if arsip.TanggalUpdate != nil {
		result.LastUpdate = arsip.TanggalUpdate
	}
	result.JenisTtdArsipDokumen = jenisList

	return result, err
}

func (s *arsipDokumenService) GetStatusByIdArsip(id uint64) (response.StatusDokumenResponse, error) {
	var result response.StatusDokumenResponse
	arsip, err := s.arsipDokumenRepository.FindArsipById(id)
	if err != nil {
		return result, err
	}
	return s.GetStatusData(arsip)
}

func (s *arsipDokumenService) FindArsipByCodeTransaction(codeTransaction string) (response.StatusDokumenResponse, error) {
	var result response.StatusDokumenResponse
	arsip, err := s.arsipDokumenRepository.FindArsipByCodeTransaction(codeTransaction)
	if err != nil {
		return result, err
	}
	return s.GetStatusData(arsip)
}

func (s *arsipDokumenService) GetStatusDokumen(id uint64) (entity.StatusDokumen, error) {
	result, err := s.arsipDokumenRepository.GetStatusDokumen(id)
	return result, err
}

// func (s *arsipDokumenService) CheckPenandatanganPemaraf(id_pengguna uint64, tipe_ttd int64, id_arsip_dokumen uint64) (entity.Pengguna, error) {
// 	result, err := s.arsipDokumenRepository.CheckPenandatanganPemaraf(id_pengguna, tipe_ttd, id_arsip_dokumen)
// 	return result, err
// }

func (s *arsipDokumenService) ValidateSignBulk(ctx *gin.Context, req []request.SignArsipDokumenRequest, penggunaService PenggunaService) error {
	var err error
	if len(req) > 0 {
		first := req[0]
		firstDokumen, err := s.FindArsipById(first.IdArsipDokumen)
		if err != nil {
			return err
		}
		penandatangan, _, _, err := penggunaService.FindPenggunaByIdComplete(first.IdUserLogin)
		if err != nil {
			return err
		}
		if first.Passphrase == "" {
			return errors.New("Passphrase kosong")
		}
		bsre, err := s.GetBsreVariable(nil)
		if err != nil {
			return err
		}
		_, _, err = util.SignInvisible(firstDokumen.PathFile, penandatangan, first.Passphrase, bsre)
		if err != nil {
			s.logEsignService.WriteLogEsign(ctx, "Pengguna dengan username: "+penandatangan.Username+" gagal melakukan penandatanganan dokumen. Pesan: "+err.Error())
			return err
		}

		//validasi open file kalau ada file yg not exist
		for _, item := range req {
			arsip, err := s.arsipDokumenRepository.FindArsipById(item.IdArsipDokumen)
			if err != nil {
				return err
			}
			file, errFile := os.Open(arsip.PathFile)
			if errFile != nil {
				fmt.Println(errFile)
				return err
			}
			defer file.Close()
		}
	} else {
		errors.New("Tidak ada dokumen di dalam request bulk ini")
	}
	//set flag_on_process=true if valid
	tx := s.DB.Begin()
	for _, i := range req {
		true_flag := true
		updatedTtd := entity.TtdArsipDokumen{Id: i.IdTtdArsipDokumen, FlagOnProcess: &true_flag}
		err = s.arsipDokumenRepository.UpdateTtdArsip(tx, updatedTtd)
		if err != nil {
			tx.Rollback()
			return err
		}
	}
	ttd, err := s.arsipDokumenRepository.FindTtdArsipById(req[0].IdTtdArsipDokumen)
	if err != nil {
		return err
	}
	err = s.logService.WriteLogAktivitas(ctx, req[0].UsernameLogin, "Penandatangan", "Penandatanganan: set flag_on_process true of list ttd",
		fmt.Sprintf("%v", req)+" | flag_on_process=null", fmt.Sprintf("%v", req)+" | flag_on_process=true", ttd.TableName())

	tx.Commit()
	return err
}

// func (s *arsipDokumenService) SignBulk(ctx *gin.Context, req []request.SignArsipDokumenRequest, ms MailService, penggunaService PenggunaService) error {
// 	tx := s.DB.Begin()
// 	// error_list := []error{}
// 	var err error
// 	for _, item := range req {
// 		err = s.ProsesTandatangan(ctx, item, ms)
// 		if err != nil {
// 			// error_list = append(error_list, err)
// 			tx.Rollback()
// 			return err
// 		}
// 	}
// 	tx.Commit()
// 	return err
// }

func (s *arsipDokumenService) SignBulk(ctx *gin.Context, req []request.SignArsipDokumenRequest, ms MailService, penggunaService PenggunaService) error {
	var err error
	// tx := s.DB.Begin()
	for _, i := range req {
		tx := s.DB.Begin()
		var detail []response.ArsipDokumenResponse
		err = errors.New("default error")
		for err != nil {
			detail, err = s.arsipDokumenRepository.GetPenggunaFromArsip(i.IdArsipDokumen, -1, -1)
			// log.Println("detail :" + fmt.Sprintf("len=%d %v\n", len(detail), detail))
		}
		var nextSigner = response.ArsipDokumenResponse{}
		nextIdSigner := -1
		if i.AlurPersetujuan == "Berjenjang" {
			if len(detail) > 0 {
				if i.IdUserLogin == detail[0].IdPenandatangan {
					log.Println("eligible to sign (berjenjang)")
					// signer = detail[0]
					if len(detail) > 1 {
						nextSigner = detail[1]
						nextIdSigner = int(detail[1].IdPenandatangan)
					}
				} else {
					fmt.Println("pengguna belum sesuai urutan yang tepat untuk menandatangani dokumen ini")
				}
			} else {
				fmt.Println("Semua penandatangan sudah melakukan tanda tangan")
			}
		}
		//UPDATE CURRENT TTD
		false_flag := false
		empty_string := ""
		input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
		updatedTtd := entity.TtdArsipDokumen{Id: i.IdTtdArsipDokumen, FlagTtd: true, FlagUrutan: &false_flag, FlagOnProcess: &false_flag,
			PenggunaUpdate: i.UsernameLogin, TanggalUpdate: &input_time}
		if updatedTtd.KeteranganSanggahTolak != &empty_string {
			updatedTtd.KeteranganSanggahTolak = &empty_string
		}
		fmt.Println("current signer: " + fmt.Sprintf("%+v", updatedTtd))
		err = errors.New("default error")
		for err != nil {
			err = s.arsipDokumenRepository.UpdateTtdArsip(tx, updatedTtd)
		}
		ttd, _ := s.arsipDokumenRepository.FindTtdArsipById(i.IdTtdArsipDokumen)
		err = errors.New("default error")
		for err != nil {
			err = s.logService.WriteLogAktivitas(ctx, i.UsernameLogin, "Penandatangan", "Penandatanganan: update flag_ttd of ttd",
				ttd.ToString(), updatedTtd.ToString(), updatedTtd.TableName())
		}

		//UPDATE NEXT TTD
		if nextIdSigner != -1 {
			true_flag := true
			updatedNextTtd := entity.TtdArsipDokumen{Id: nextSigner.IdTtdArsipDokumen, FlagUrutan: &true_flag}
			fmt.Println("next signer if exists: " + fmt.Sprintf("%+v", updatedNextTtd))
			err = errors.New("default error")
			for err != nil {
				err = s.arsipDokumenRepository.UpdateTtdArsip(tx, updatedNextTtd)
			}
			// writing to log: set flag_urutan true of next ttd_arsip_dokumen (Berjenjang)
			nextTtd, _ := s.arsipDokumenRepository.FindTtdArsipById(nextSigner.IdTtdArsipDokumen)
			err = errors.New("default error")
			for err != nil {
				err = s.logService.WriteLogAktivitas(ctx, i.UsernameLogin, "Penandatangan", "Penandatanganan: set flag_urutan true of next ttd",
					nextTtd.ToString(), updatedNextTtd.ToString(), updatedNextTtd.TableName())
			}
		}

		countUnsigned := -1
		err = errors.New("default error")
		for err != nil {
			countUnsigned, err = s.arsipDokumenRepository.CountUnsignedArsip(int(i.IdArsipDokumen), int(i.IdTtdArsipDokumen))
		}

		updatedArsip := entity.ArsipDokumen{Id: i.IdArsipDokumen, PenggunaUpdate: i.UsernameLogin}

		if countUnsigned == 0 {
			fmt.Println("update status arsip to Selesai")
			updatedArsip.IdStatusDokumen = 3
		} else {
			fmt.Println("update status arsip to Diproses")
			updatedArsip.IdStatusDokumen = 2
			if i.AlurPersetujuan == "Berjenjang" {
				if nextIdSigner != -1 {
					err = errors.New("default error")
					for err != nil {
						err = ms.SignerMail(nextSigner, i.NamaJenisDokumen, i.UsernamePengaju, "/dokumen/detail/"+strconv.FormatUint(i.IdArsipDokumen, 10))
						fmt.Println("email sent to: " + nextSigner.EmailPenandatangan)
					}
				}
			}
		}

		var penandatangan entity.Pengguna
		err = errors.New("default error")
		for err != nil {
			penandatangan, _, _, err = s.penggunaService.FindPenggunaByIdComplete(i.IdUserLogin)
		}
		// [START BLOK TTD BSRE]
		var arsip entity.ArsipDokumen
		err = errors.New("default error")
		for err != nil {
			arsip, err = s.arsipDokumenRepository.FindArsipById(i.IdArsipDokumen)
		}
		var result_file []byte
		var id_dokumen_bsre string
		var bsre util.BSRE
		err = errors.New("default error")
		for err != nil {
			bsre, err = s.GetBsreVariable(nil)
		}
		err = errors.New("default error")
		for err != nil {
			result_file, id_dokumen_bsre, err = util.SignInvisible(arsip.PathFile, penandatangan, i.Passphrase, bsre)
		}
		err = errors.New("default error")
		for err != nil {
			err = os.Remove(arsip.PathFile)
		}
		filename := strconv.FormatUint(i.IdArsipDokumen, 10) + "_" + id_dokumen_bsre + ".pdf"
		targetPath := filepath.Join(util.BASE_PATH, util.DOCS_PATH, filename)
		fmt.Println("saving file from bsre to: " + targetPath)
		//save file baru dari bsre
		err = errors.New("default error")
		for err != nil {
			err = os.WriteFile(targetPath, result_file, 0644)
		}

		input_time = time.Now().In(time.FixedZone("UTC+7", 7*60*60))
		updatedArsip.FileDokumen = filename
		updatedArsip.PathFile = targetPath
		updatedArsip.IdDokumenBsre = id_dokumen_bsre
		updatedArsip.TanggalUpdate = &input_time
		err = errors.New("default error")
		for err != nil {
			err = s.arsipDokumenRepository.UpdateArsip(tx, updatedArsip)
		}
		err = errors.New("default error")
		for err != nil {
			err = s.logService.WriteLogAktivitas(ctx, i.UsernameLogin, "Penandatangan", "Penandatanganan: update status arsip",
				arsip.ToString(), updatedArsip.ToString(), updatedArsip.TableName())
		}
		if countUnsigned == 0 && arsip.SourceApk != "internal" {
			err = s.SendCallbackBulk(ctx, i, arsip, filename, targetPath, result_file)
			if err != nil {
				fmt.Println("error", err)
			}
		}
		tx.Commit()
	}
	// tx.Commit()
	return err
}

// func (s *arsipDokumenService) ProsesTandatanganBulk(tx *gorm.DB, ctx *gin.Context, req request.SignArsipDokumenRequest, ms MailService) error {

// }

func (s *arsipDokumenService) GetFinishedListBulk(id_penandatangan_login int) ([]response.ArsipDokumenResponse, error) {
	result, err := s.arsipDokumenRepository.GetFinishedListBulk(id_penandatangan_login)
	return result, err
}

func (s *arsipDokumenService) RemoveNotificationBulk(ctx *gin.Context, req []request.SignArsipDokumenRequest) error {
	var err error
	tx := s.DB.Begin()
	_, _, err = util.ExtractUsernameListPeranByContext(ctx)
	if err != nil || !util.IsRequestAuthorizableByRole(ctx, "A03") {
		return errors.New("Unauthorized, Anda bukan Penandatangan!")
	}
	for _, i := range req {
		// updatedTtd := entity.TtdArsipDokumen{Id: i.IdTtdArsipDokumen, FlagOnProcess: nil}
		// err = s.arsipDokumenRepository.UpdateTtdArsip(tx, updatedTtd)
		err = s.arsipDokumenRepository.RemoveNotificationBulk(int(i.IdTtdArsipDokumen))
		if err != nil {
			tx.Rollback()
			return err
		}
	}
	tx.Commit()
	return err
}

func (s *arsipDokumenService) Sign(ctx *gin.Context, req request.SignArsipDokumenRequest, ms MailService) error {
	tx := s.DB.Begin()
	err := s.ProsesTandatangan(ctx, req, ms)
	if err != nil {
		tx.Rollback()
		return err
	}
	tx.Commit()
	return err
}

func (s *arsipDokumenService) ProsesTandatangan(ctx *gin.Context, req request.SignArsipDokumenRequest, ms MailService) error {
	tx := s.DB.Begin()
	detail, err := s.arsipDokumenRepository.GetPenggunaFromArsip(req.IdArsipDokumen, -1, -1)
	log.Println("detail :" + fmt.Sprintf("len=%d %v\n", len(detail), detail))
	if err != nil {
		return err
	}
	var nextSigner = response.ArsipDokumenResponse{}
	nextIdSigner := -1
	// if (signer != response.ArsipDokumenResponse{}) {
	// 	log.Println("eligible to sign")
	if req.AlurPersetujuan == "Berjenjang" {
		if len(detail) > 0 {
			if req.IdUserLogin == detail[0].IdPenandatangan {
				log.Println("eligible to sign (berjenjang)")
				// signer = detail[0]
				if len(detail) > 1 {
					nextSigner = detail[1]
					nextIdSigner = int(detail[1].IdPenandatangan)
				}
			} else {
				return errors.New("pengguna belum sesuai urutan yang tepat untuk menandatangani dokumen ini")
			}
		} else {
			return errors.New("Semua penandatangan sudah melakukan tanda tangan")
		}

	}
	// } else {
	// 	log.Println("not eligible to sign")
	// 	return errors.New("pengguna tidak berhak untuk menandatangani dokumen ini")
	// }

	updatedTtd := entity.TtdArsipDokumen{}
	updatedTtd.Id = req.IdTtdArsipDokumen
	updatedTtd.FlagTtd = true
	false_flag := false
	updatedTtd.FlagUrutan = &false_flag
	empty_string := ""
	if updatedTtd.KeteranganSanggahTolak != &empty_string {
		updatedTtd.KeteranganSanggahTolak = &empty_string
	}
	updatedTtd.PenggunaUpdate = req.UsernameLogin
	input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	updatedTtd.TanggalUpdate = &input_time
	fmt.Println("current signer: " + fmt.Sprintf("%+v", updatedTtd))
	err = s.arsipDokumenRepository.UpdateTtdArsip(tx, updatedTtd)
	if err != nil {
		tx.Rollback()
		return err
	}

	// writing to log: set flag_ttd true and flag_urutan false of current ttd_arsip_dokumen (all alurpersetujuan)
	ttd, err := s.arsipDokumenRepository.FindTtdArsipById(req.IdTtdArsipDokumen)
	if err != nil {
		return err
	}
	if err = s.logService.WriteLogAktivitas(ctx, req.UsernameLogin, "Penandatangan", "Penandatanganan: update flag_ttd of ttd",
		ttd.ToString(), updatedTtd.ToString(), updatedTtd.TableName()); err != nil {
		// tx.Rollback()
		return err
	}
	if nextIdSigner != -1 {
		updatedNextTtd := entity.TtdArsipDokumen{}
		updatedNextTtd.Id = nextSigner.IdTtdArsipDokumen
		true_flag := true
		updatedNextTtd.FlagUrutan = &true_flag
		fmt.Println("next signer if exists: " + fmt.Sprintf("%+v", updatedNextTtd))
		err = s.arsipDokumenRepository.UpdateTtdArsip(tx, updatedNextTtd)
		if err != nil {
			tx.Rollback()
			return err
		}
		// writing to log: set flag_urutan true of next ttd_arsip_dokumen (Berjenjang)
		nextTtd, err := s.arsipDokumenRepository.FindTtdArsipById(nextSigner.IdTtdArsipDokumen)
		if err != nil {
			// tx.Rollback()
			return err
		}
		if err = s.logService.WriteLogAktivitas(ctx, req.UsernameLogin, "Penandatangan", "Penandatanganan: set flag_urutan true of next ttd",
			nextTtd.ToString(), updatedNextTtd.ToString(), updatedNextTtd.TableName()); err != nil {
			tx.Rollback()
			return err
		}
	}

	arsip, err := s.arsipDokumenRepository.FindArsipById(req.IdArsipDokumen)
	if err != nil {
		tx.Rollback()
		return err
	}

	countUnsigned, err := s.arsipDokumenRepository.CountUnsignedArsip(int(req.IdArsipDokumen), int(req.IdTtdArsipDokumen))
	// unsignedTtd, err := s.arsipDokumenRepository.GetUnsignedArsip(int(req.IdArsipDokumen), int(req.IdTtdArsipDokumen))
	if err != nil {
		return err
	}
	// unsigned := 0
	// for _, item := range unsignedTtd {
	// 	if !item.FlagTtd {
	// 		unsigned++
	// 	}
	// 	if item.Keterangan != "" {

	// 	}
	// }
	fmt.Println("countUnsigned: ", countUnsigned)

	updatedArsip := entity.ArsipDokumen{}
	updatedArsip.Id = req.IdArsipDokumen
	updatedArsip.PenggunaUpdate = req.UsernameLogin
	input_time = time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	updatedArsip.TanggalUpdate = &input_time

	// fmt.Println("countUnsigned: ", countUnsigned)
	// [START BLOK TTD BSRE]
	var result_file []byte
	var id_dokumen_bsre string
	if countUnsigned == 0 {
		fmt.Println("update status arsip to Selesai")
		updatedArsip.IdStatusDokumen = 3
	} else {
		fmt.Println("update status arsip to Diproses")
		updatedArsip.IdStatusDokumen = 2
		if req.AlurPersetujuan == "Berjenjang" {
			if (nextSigner != response.ArsipDokumenResponse{}) {
				err = ms.SignerMail(nextSigner, req.NamaJenisDokumen, req.UsernamePengaju, "/dokumen/detail/"+strconv.FormatUint(req.IdArsipDokumen, 10))
				fmt.Println("email sent to: " + nextSigner.EmailPenandatangan)
				if err != nil {
					// tx.Rollback()
					return err
				}
			}
		}
	}

	result_file, id_dokumen_bsre, err = s.ApiBsreSign(ctx, tx, arsip.PathFile, req.IdUserLogin, req.Passphrase)
	if err != nil {
		tx.Rollback()
		return err
	}

	//backup file lama
	backupFile, err := os.ReadFile(arsip.PathFile) //read the content of file
	if err != nil {
		return err
	}

	//delete file lama
	fmt.Println("deleting old file: " + arsip.PathFile)
	if err := os.Remove(arsip.PathFile); err != nil {
		tx.Rollback()
		return err
	}
	filename := strconv.FormatUint(req.IdArsipDokumen, 10) + "_" + id_dokumen_bsre + ".pdf"
	targetPath := filepath.Join(util.BASE_PATH, util.DOCS_PATH, filename)

	fmt.Println("saving file from bsre to: " + targetPath)
	//save file baru dari bsre
	err = os.WriteFile(targetPath, result_file, 0644)
	if err != nil {
		tx.Rollback()
		fmt.Println("Error writing PDF file:", err)
		return err
	}

	if countUnsigned == 0 && arsip.SourceApk != "internal" {
		parameterValue := strings.ToUpper(arsip.SourceApk)
		pengaturanEksternal, err := s.pengaturanEksternalService.GetPengaturanEksternalByField(ctx, "system", "nama_app", parameterValue)
		if err != nil {
			tx.Rollback()
			backupErr := s.RetrieveBackupFile(backupFile, arsip.PathFile, targetPath)
			if backupErr != nil {
				fmt.Println("Error retrieving backup file")
			}
			s.logService.WriteLogAktivitas(ctx, req.UsernameLogin, "Penandatangan", "Pengaturan Eksternal API untuk aplikasi "+parameterValue+" gagal diakses pada percobaan pengiriman berkas final ini. Harap segera cek.", arsip.ToString(), err.Error(), "pengaturan")
			return errors.New("Terjadi kesalahan pada konfigurasi API External sistem. Silakan hubungi Administrator")
		}

		apiTarget := pengaturanEksternal.CallBack
		// pengaturan, err := s.pengaturanService.GetPengaturanByField(nil, "system", "variable", parameterValue)
		// apiTarget := pengaturan.Value

		// var requestBody bytes.Buffer
		// writer := multipart.NewWriter(&requestBody)
		// fileWriter, err := writer.CreateFormFile("file", filename)

		// if err != nil {
		// 	return nil, err
		// }

		// _, err = fileWriter.Write(result_file)
		// if err != nil {
		// 	return nil, err
		// }

		// if parameterValue == "SIKERJASAMA" {
		// 	err = writer.WriteField("IdDokumenEsign", strconv.Itoa(int(req.IdArsipDokumen)))
		// 	if err != nil {
		// 		tx.Rollback()
		// 		backupErr := s.RetrieveBackupFile(backupFile, arsip.PathFile, targetPath)
		// 		if backupErr != nil {
		// 			fmt.Println("Error retrieving backup file")
		// 		}
		// 		return err
		// 	}

		// 	err = writer.WriteField("NoDokumen", arsip.NomorSurat)
		// 	if err != nil {
		// 		tx.Rollback()
		// 		backupErr := s.RetrieveBackupFile(backupFile, arsip.PathFile, targetPath)
		// 		if backupErr != nil {
		// 			fmt.Println("Error retrieving backup file")
		// 		}
		// 		return err
		// 	}
		// }

		request, err := s.requestSetup(pengaturanEksternal, arsip, req, filename, result_file)
		if err != nil {
			tx.Rollback()
			backupErr := s.RetrieveBackupFile(backupFile, arsip.PathFile, targetPath)
			if backupErr != nil {
				fmt.Println("Error retrieving backup file")
			}
			return err
		}

		//SSO Token & additional headers
		// request.Header.Add("Authorization", util.ExtractToken(ctx))
		// request.Header.Set("Content-Type", writer.FormDataContentType())
		// request.Header.Set("id_dokumen", strconv.Itoa(int(req.IdArsipDokumen)))
		log.Println("Authorization: " + request.Header.Get("Authorization"))
		log.Println("Content-Type: " + request.Header.Get("Content-Type"))
		// if parameterValue == "SIKERJASAMA" {
		// 	request.Header.Set("x-api-key", "Dei5tqbqsz6Plq5qOfm8udcLEW0DxL27o9UtSn5CgdGZwLi+rc3TfB07wLN/9oyrPqZayRjLolArNCrrIlU34Q==")
		// 	util.AddBasicAuth("esign", "es1gnT0Kerj4s4m4177", request)
		// }
		var client = &http.Client{}

		request.Method = "POST"
		response, err := client.Do(request)

		if err != nil {
			tx.Rollback()
			fmt.Println("Error send document to another application: ", err)
			backupErr := s.RetrieveBackupFile(backupFile, arsip.PathFile, targetPath)
			if backupErr != nil {
				fmt.Println("Error retrieving backup file")
			}
			return err
		}

		fmt.Println("send document " + targetPath + " to : " + apiTarget)
		fmt.Println("res: " + response.Status)

		if response.StatusCode != http.StatusOK && response.StatusCode != http.StatusCreated {
			// Baca pesan kesalahan jika ada
			// Print request body
			if request.Body != nil {
				bodyBytes, err := io.ReadAll(request.Body)
				if err != nil {
					fmt.Println("Error reading request body:", err)
				} else {
					fmt.Println("Request Body after fail:", string(bodyBytes))
				}
			} else {
				fmt.Println("Request Body: null")
			}

			//Print Field Request Body
			// err = request.ParseMultipartForm(int64(32 << 20))
			// if err != nil {
			// 	fmt.Println("Error parsing multipart form:", err)
			// }

			// // Access the value of the request body
			// for key, values := range request.MultipartForm.Value {
			// 	fmt.Printf("Field: %s, Values: %v\n", key, values)
			// }

			// Print request headers
			fmt.Println("Request Headers:", request.Header)

			// Print response headers
			fmt.Println("Response Headers:", response.Header)

			// Print response body
			bodyBytes, err := io.ReadAll(response.Body)
			if err != nil {
				fmt.Println("Error reading response body:", err)
			} else {
				fmt.Println("Response Body:", string(bodyBytes))
			}

			tx.Rollback()
			backupErr := s.RetrieveBackupFile(backupFile, arsip.PathFile, targetPath)
			if backupErr != nil {
				fmt.Println("Error retrieving backup file")
			}
			return errors.New("response status code to callback is not 200 OK: " + response.Status + " with body: " + string(bodyBytes))

		}
	}

	updatedArsip.FileDokumen = filename
	updatedArsip.PathFile = targetPath
	updatedArsip.IdDokumenBsre = id_dokumen_bsre
	input_time = time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	updatedArsip.TanggalUpdate = &input_time
	// [END BLOK TTD BSRE]

	err = s.arsipDokumenRepository.UpdateArsip(tx, updatedArsip)
	if err != nil {
		tx.Rollback()
		backupErr := s.RetrieveBackupFile(backupFile, arsip.PathFile, targetPath)
		if backupErr != nil {
			fmt.Println("Error retrieving backup file")
		}
		return err
	}

	// writing to log: set id_status_dokumen to 2 (diproses) or 3(selesai) of current arsip_dokumen
	if err = s.logService.WriteLogAktivitas(ctx, req.UsernameLogin, "Penandatangan", "Penandatanganan: update status arsip",
		arsip.ToString(), updatedArsip.ToString(), updatedArsip.TableName()); err != nil {
		// tx.Rollback()
		backupErr := s.RetrieveBackupFile(backupFile, arsip.PathFile, targetPath)
		if backupErr != nil {
			fmt.Println("Error retrieving backup file")
		}
		return err
	}
	tx.Commit()
	return err
}

func (s *arsipDokumenService) RetrieveBackupFile(backupFile []byte, backupFilePath string, currentFilePath string) error {
	var err error
	fmt.Println("Removing current file at: " + currentFilePath)
	if err := os.Remove(currentFilePath); err != nil {
		return err
	}

	fmt.Println("Retrieving old file as backup:", backupFilePath)
	err = os.WriteFile(backupFilePath, backupFile, 0644)
	if err != nil {
		return err
	}
	return err
}

func (s *arsipDokumenService) VerifyFile(ctx *gin.Context) (response.VerifyBSREResponse, error) {
	var response response.VerifyBSREResponse
	bsre, err := s.GetBsreVariable(nil)
	if err != nil {
		return response, err
	}
	verify, err := util.VerifyFile(ctx, bsre)
	if err != nil {
		return response, err
	}
	return verify, err
}

func (s *arsipDokumenService) VerifyFileById(req request.VerifyFileRequest) (response.VerifyByDokumenBSREResponse, error) {
	var response response.VerifyByDokumenBSREResponse
	if req.Tipe == "dokumen" {
		arsip, err := s.arsipDokumenRepository.FindArsipById(req.Id)
		if err != nil {
			return response, err
		}
		response.NomorSurat = arsip.NomorSurat
		response.TanggalPengajuan = arsip.TanggalEntry
		response.Url = os.Getenv("BASEURL_FRONTEND") + "/dokumen/detail/" + strconv.Itoa(int(req.Id))
	} else if req.Tipe == "batch" {
		arsipBatch, err := s.batchDokumenRepository.FindDokumenById(int(req.Id))
		if err != nil {
			return response, err
		}
		batch, err := s.batchDokumenRepository.FindBatchByIdBatch(arsipBatch.IdBatch)
		if err != nil {
			return response, err
		}
		response.NomorSurat = arsipBatch.NomorSurat
		response.TanggalPengajuan = batch.TanggalEntry
		response.Url = os.Getenv("BASEURL_FRONTEND") + "/batch/detail/" + strconv.Itoa(int(req.Id))
	} else {
		return response, errors.New("tipe tidak dikenal")
	}

	pathFile, err := s.arsipDokumenRepository.GetDokumenPathFileById(req.Id, req.Tipe)
	if err != nil {
		return response, err
	}
	bsre, err := s.GetBsreVariable(nil)
	if err != nil {
		return response, err
	}
	verify, err := util.VerifyFileByPath(bsre, pathFile)
	if err != nil {
		return response, err
	}
	response.VerifyBSREResponse = verify
	return response, err
}

func (s *arsipDokumenService) GenerateNomorPengajuan(kode_org string) (string, error) {
	// kode_organisasi/ddmmyyyy/nomor_urut
	kode_org = strings.ReplaceAll(strings.ToUpper(kode_org), " ", "_")
	var generateErr error
	now := time.Now()
	// today := now.Format("2006-01-02")
	today := now.Format("02-01-2006")
	today_counter, err := s.arsipDokumenRepository.GetTodayCounter(today)
	if err != nil {
		today_counter = ""
		generateErr = err
	}

	// fmt.Println("today_counter  ", today_counter)
	// fmt.Println("today  ", today)
	today = strings.ReplaceAll(today, "-", "")

	// fmt.Println("today strip  ", today)

	// today_arr := strings.Split(today, "-")
	// today = today_arr[2] + today_arr[1] + today_arr[0]
	// if err != nil {
	// 	today_counter = ""
	// 	generateErr = err
	// }
	// fmt.Println("kode_org  ", kode_org)
	nomor_pengajuan := kode_org + "/" + today + "/" + today_counter
	return nomor_pengajuan, generateErr
}

func (s *arsipDokumenService) GetEligibleSigner(id_user_login uint64, detail []response.ArsipDokumenResponse) response.ArsipDokumenResponse {
	var signer = response.ArsipDokumenResponse{}
	for _, item := range detail {
		if item.IdPenandatangan == id_user_login {
			signer = item
			break
		}
	}
	return signer
}

func (s *arsipDokumenService) SendCallbackBulk(ctx *gin.Context, req request.SignArsipDokumenRequest, arsip entity.ArsipDokumen, filename string, targetPath string, result_file []byte) error {
	var err error
	parameterValue := strings.ToUpper(arsip.SourceApk)
	pengaturanEksternal, err := s.pengaturanEksternalService.GetPengaturanEksternalByField(ctx, "system", "nama_app", parameterValue)
	if err != nil {
		s.logService.WriteLogAktivitas(ctx, req.UsernameLogin, "Penandatangan", "Pengaturan Eksternal API untuk aplikasi "+parameterValue+" gagal diakses pada percobaan pengiriman berkas final ini. Harap segera cek.", arsip.ToString(), err.Error(), "pengaturan")
		return errors.New("Terjadi kesalahan pada konfigurasi API External sistem. Silakan hubungi Administrator")
	}

	apiTarget := pengaturanEksternal.CallBack
	// pengaturan, err := s.pengaturanService.GetPengaturanByField(nil, "system", "variable", parameterValue)
	// apiTarget := pengaturan.Value

	// var requestBody bytes.Buffer
	// writer := multipart.NewWriter(&requestBody)
	// fileWriter, err := writer.CreateFormFile("file", filename)

	// if err != nil {
	// 	return err
	// }

	// _, err = fileWriter.Write(result_file)
	// if err != nil {
	// 	panic(err)
	// }

	// if parameterValue == "SIKERJASAMA" {
	// 	err = writer.WriteField("IdDokumenEsign", strconv.Itoa(int(req.IdArsipDokumen)))
	// 	if err != nil {
	// 		return err
	// 	}

	// 	err = writer.WriteField("NoDokumen", arsip.NomorSurat)
	// 	if err != nil {
	// 		return err
	// 	}
	// }

	request, err := s.requestSetup(pengaturanEksternal, arsip, req, filename, result_file)
	if err != nil {
		return err
	}

	//SSO Token
	// request.Header.Add("Authorization", util.ExtractToken(ctx))
	// request.Header.Set("Content-Type", writer.FormDataContentType())
	// request.Header.Set("id_dokumen", strconv.Itoa(int(req.IdArsipDokumen)))
	log.Println("Authorization: " + request.Header.Get("Authorization"))
	log.Println("Content-Type: " + request.Header.Get("Content-Type"))
	// if parameterValue == "SIKERJASAMA" {
	// 	request.Header.Set("x-api-key", "Dei5tqbqsz6Plq5qOfm8udcLEW0DxL27o9UtSn5CgdGZwLi+rc3TfB07wLN/9oyrPqZayRjLolArNCrrIlU34Q==")
	// 	util.AddBasicAuth("esign", "es1gnT0Kerj4s4m4177", request)
	// }
	var client = &http.Client{}

	response, err := client.Do(request)

	if err != nil {
		return err
	}

	fmt.Println("send document " + targetPath + " to : " + apiTarget)
	fmt.Println("res: " + response.Status)

	if response.StatusCode != http.StatusOK {
		// Baca pesan kesalahan jika ada
		// Print all headers
		headers, err := json.MarshalIndent(request.Header, "", "  ")
		if err != nil {
			fmt.Println("Error formatting headers:", err)
		}
		fmt.Println("Headers:", string(headers))
		var bodyString string
		// Print body
		if request.Body != nil {
			var body interface{}
			err := json.NewDecoder(request.Body).Decode(&body)
			if err != nil {
				fmt.Println("Error decoding body:", err)
			}
			bodyJson, err := json.MarshalIndent(body, "", "  ")
			if err != nil {
				fmt.Println("Error formatting body:", err)
			}
			fmt.Println("Body:", string(bodyJson))
			bodyString = string(bodyJson)
		} else {
			fmt.Println("Body: null")
			bodyString = "null"
		}

		return errors.New("response status code to batch callback is not 200 OK: " + response.Status + " with body: " + bodyString)
	}
	return err
}

func (s *arsipDokumenService) SendEmail(tx *gorm.DB, req entity.ArsipDokumen, ms MailService, alurPersetujuan string, namaDokumen string) (entity.ArsipDokumen, error) {
	var err error

	//get pengguna pertama yg akan diemail oleh krn itu urutan_ttd = 1
	// detail, err := s.GetPenggunaFromArsip(uint64(req.Id), -1, 1)
	detail := []response.ArsipDokumenResponse{}

	// for _, ttd := range req.TtdArsipDokumen {
	// 	true_flag := true
	// 	if alurPersetujuan == "Berjenjang" {
	// 		if ttd.Id == detail[0].IdTtdArsipDokumen {
	// 			ttd.FlagUrutan = &true_flag
	// 			break
	// 		}
	// 	} else {
	// 		ttd.FlagUrutan = &true_flag
	// 	}
	// 	err = s.arsipDokumenRepository.UpdateTtdArsip(tx, ttd)
	// }

	// if err != nil {
	// 	// tx.Rollback()
	// 	return req, err
	// }

	log.Println("req.TtdArsipDokumen :" + fmt.Sprintf("len=%d %v\n", len(req.TtdArsipDokumen), req.TtdArsipDokumen))
	for _, ttd := range req.TtdArsipDokumen {
		log.Println("ttd id :", ttd.Id)
		log.Println("flagurutan :", ttd.FlagUrutan)
		log.Println("*flagurutan :", *ttd.FlagUrutan)
		// true_flag := true
		if *ttd.FlagUrutan {
			person, err := s.penggunaService.FindPenggunaById(ttd.IdPengguna)
			fmt.Println("email will be sent to penandatangan: ", person)
			detail = append(detail, response.ArsipDokumenResponse{
				NamaPenandatangan: person.NamaLengkap, EmailPenandatangan: person.Email})
			if err != nil {
				tx.Rollback()
				return req, err
			}
		}
	}

	log.Println("detail :" + fmt.Sprintf("len=%d %v\n", len(detail), detail))

	for _, p := range detail {
		err = ms.SignerMail(p, namaDokumen, req.PenggunaEntry,
			"/dokumen/detail/"+strconv.FormatUint(req.Id, 10))
		log.Println("email sent to: " + p.EmailPenandatangan)
		if err != nil {
			tx.Rollback()
			return req, err
		}
	}
	//send notif to pengaju
	pengaju, err := s.penggunaService.FindPenggunaByUsername(req.PenggunaEntry)
	err = ms.NotifyCreateToPengaju(namaDokumen, pengaju, "/dokumen/detail/"+strconv.FormatUint(req.Id, 10), int(req.Id))
	log.Println("email sent to pengaju: " + req.PenggunaEntry)
	if err != nil {
		tx.Rollback()
		return req, err
	}
	return req, err
}

// func (s *arsipDokumenService) SendBatchEmail(req entity.ArsipDokumen, ms MailService, alurPersetujuan string, namaDokumen string, batchId string) error {
// 	var err error
// 	detail := []response.ArsipDokumenResponse{}

// 	log.Println("send batch email with ttd:" + fmt.Sprintf("len=%d %v\n", len(req.TtdArsipDokumen), req.TtdArsipDokumen))
// 	for _, ttd := range req.TtdArsipDokumen {
// 		log.Println("ttd id :", ttd.Id)
// 		log.Println("ttd.IdPengguna", ttd.IdPengguna)
// 		log.Println("*flagurutan :", *ttd.FlagUrutan)
// 		log.Println("flagurutan :", ttd.FlagUrutan)
// 		// true_flag := true
// 		if *ttd.FlagUrutan {
// 			person, err := s.penggunaService.FindPenggunaById(ttd.IdPengguna)
// 			fmt.Println("batch email will be sent to: ", person)
// 			detail = append(detail, response.ArsipDokumenResponse{
// 				NamaPenandatangan: person.NamaLengkap, EmailPenandatangan: person.Email})
// 			if err != nil {
// 				return err
// 			}
// 		}
// 	}

// 	log.Println("detail :" + fmt.Sprintf("len=%d %v\n", len(detail), detail))

// 	for _, p := range detail {
// 		err = ms.SignerMail(p, namaDokumen, req.PenggunaEntry, "/batch-sign?batchId="+batchId)
// 		log.Println("email sent to: " + p.EmailPenandatangan)
// 		if err != nil {
// 			return err
// 		}
// 	}
// 	return err
// }

func (s *arsipDokumenService) GetOrganisasiByPenggunaId(id uint64) (response.OrganisasiResponse, error) {
	result, err := s.arsipDokumenRepository.GetOrganisasiByPenggunaId(id)
	return result, err
}

func (s *arsipDokumenService) CancelArsip(ctx *gin.Context, ms MailService, id uint64) error {
	tx := s.DB.Begin()
	arsip, err := s.arsipDokumenRepository.FindArsipById(id)
	if err != nil {
		return err
	}

	username, _, err := util.ExtractUsernameListPeranByContext(ctx)
	if err != nil || !util.IsRequestAuthorizableByRole(ctx, "A02") {
		tx.Rollback()
		return errors.New("Unauthorized, Anda bukan Pengaju!")
	}
	fmt.Println("soft delete auth username: ", username)

	for _, ttd := range arsip.TtdArsipDokumen {
		updatedTtd := entity.TtdArsipDokumen{}
		false_flag := false
		updatedTtd.Id = ttd.Id
		updatedTtd.FlagUrutan = &false_flag
		updatedTtd.PenggunaUpdate = username
		input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
		updatedTtd.TanggalUpdate = &input_time
		err = s.arsipDokumenRepository.UpdateTtdArsip(tx, updatedTtd)
		if err != nil {
			tx.Rollback()
			return err
		}
		if err = s.logService.WriteLogAktivitas(ctx, username, "Pengaju", "Soft delete dokumen set flag_urutan to false di semua ttd_arsip",
			ttd.ToString(), updatedTtd.ToString(), ttd.TableName()); err != nil {
			return err
		}
	}

	jenisDokumen, err := s.arsipDokumenRepository.FindJenisDokumenByIdJDO(arsip.IdJenisDokumenOrganisasi)
	if err != nil {
		return err
	}

	updatedArsip := entity.ArsipDokumen{}
	updatedArsip.Id = arsip.Id
	updatedArsip.IdStatusDokumen = 4
	input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	updatedArsip.TanggalUpdate = &input_time
	updatedArsip.PenggunaUpdate = username
	fmt.Println("soft delete arsip: ", updatedArsip)
	err = s.arsipDokumenRepository.UpdateArsip(tx, updatedArsip)
	if err != nil {
		tx.Rollback()
		return err
	}
	if err = s.logService.WriteLogAktivitas(ctx, username, "Pengaju", "Soft delete dokumen",
		arsip.ToString(), updatedArsip.ToString(), arsip.TableName()); err != nil {
		return err
	}
	penandatanganList := []response.PenggunaResponse{}
	for _, ttd := range arsip.TtdArsipDokumen {
		person, err := s.penggunaService.FindPenggunaById(ttd.IdPengguna)
		if err != nil {
			return err
		}
		penandatanganList = append(penandatanganList, response.PenggunaResponse{
			NamaLengkap: person.NamaLengkap, Email: person.Email})

	}
	// pengguna, err := s.arsipDokumenRepository.GetPenggunaFromArsip(id, -1, -1)
	for _, p := range penandatanganList {
		err = ms.NotifyCancelToPenandatangan(p, jenisDokumen.Nama, username,
			"/dokumen/detail/"+strconv.FormatUint(id, 10))
		log.Println("email sent to: " + p.Email)
		if err != nil {
			tx.Rollback()
			return err
		}
	}
	tx.Commit()
	return err
}

func (s *arsipDokumenService) RejectArsip(ctx *gin.Context, ms MailService, req request.RejectRequest) error {
	tx := s.DB.Begin()
	ttd, err := s.arsipDokumenRepository.FindTtdArsipById(req.IdTtdArsipDokumen)
	if err != nil {
		return err
	}

	username, _, err := util.ExtractUsernameListPeranByContext(ctx)
	if err != nil || !util.IsRequestAuthorizableByRole(ctx, "A03") {
		tx.Rollback()
		return errors.New("Unauthorized, Anda bukan Penandatangan!")
	}
	updatedTtd := entity.TtdArsipDokumen{}
	true_flag := true
	updatedTtd.Id = ttd.Id
	updatedTtd.Keterangan = &req.Keterangan
	updatedTtd.CurrentReject = &true_flag
	// updatedTtd.FlagTtd = true
	// updatedTtd.FlagUrutan = &false_flag
	empty_string := ""
	if updatedTtd.KeteranganSanggahTolak != &empty_string {
		updatedTtd.KeteranganSanggahTolak = &empty_string
	}
	updatedTtd.PenggunaUpdate = ttd.Pengguna.Username
	input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	updatedTtd.TanggalUpdate = &input_time
	fmt.Println("reject ttdarsip: ", fmt.Sprintf("%+v", updatedTtd))
	err = s.arsipDokumenRepository.UpdateTtdArsip(tx, updatedTtd)
	if err != nil {
		tx.Rollback()
		return err
	}
	if err = s.logService.WriteLogAktivitas(ctx, username, "Penandatangan", "Reject Dokumen",
		ttd.ToString(), updatedTtd.ToString(), ttd.TableName()); err != nil {
		return err
	}

	person, err := s.penggunaService.FindPenggunaByUsername(ttd.PenggunaEntry)
	if err != nil {
		return err
	}
	pengaju := response.PenggunaResponse{NamaLengkap: person.NamaLengkap, Email: person.Email}
	err = ms.NotifyRejectToPengaju(pengaju, req.NamaJenisDokumen, username, "/dokumen/detail/"+strconv.FormatUint(req.IdArsipDokumen, 10))
	fmt.Println("email sent to: " + pengaju.Email)
	if err != nil {
		// tx.Rollback()
		return err
	}

	// var nextTtd entity.TtdArsipDokumen

	// if req.NextIdTtdArsipDokumen != -1 {
	// 	updatedNextTtd := entity.TtdArsipDokumen{}
	// 	updatedNextTtd.Id = uint64(req.NextIdTtdArsipDokumen)
	// 	true_flag := true
	// 	updatedNextTtd.FlagUrutan = &true_flag
	// 	fmt.Println("next signer if exists: " + fmt.Sprintf("%+v", updatedNextTtd))
	// 	err = s.arsipDokumenRepository.UpdateTtdArsip(tx, updatedNextTtd)
	// 	if err != nil {
	// 		tx.Rollback()
	// 		return err
	// 	}
	// 	// writing to log: set flag_urutan true of next ttd_arsip_dokumen (Berjenjang)
	// 	nextTtd, err = s.arsipDokumenRepository.FindTtdArsipById(uint64(req.NextIdTtdArsipDokumen))
	// 	if err != nil {
	// 		return err
	// 	}
	// 	if err = s.logService.WriteLogAktivitas(ctx, username, "Penandatangan", "Penandatanganan: set flag_urutan true of next ttd",
	// 		nextTtd.ToString(), updatedNextTtd.ToString(), updatedNextTtd.TableName()); err != nil {
	// 		tx.Rollback()
	// 		return err
	// 	}

	// }

	arsip, err := s.arsipDokumenRepository.FindArsipById(req.IdArsipDokumen)
	if err != nil {
		tx.Rollback()
		return err
	}

	// countUnsigned, err := s.arsipDokumenRepository.CountUnsignedArsip(int(req.IdArsipDokumen), int(req.IdTtdArsipDokumen))
	// // unsignedTtd, err := s.arsipDokumenRepository.GetUnsignedArsip(int(req.IdArsipDokumen), int(req.IdTtdArsipDokumen))
	// if err != nil {
	// 	return err
	// }
	// if len(arsip.TtdArsipDokumen) == 1 {
	// 	countUnsigned = 1
	// }
	// fmt.Println("unsigned: ", countUnsigned)

	// updatedArsip := entity.ArsipDokumen{}
	// updatedArsip.Id = req.IdArsipDokumen
	// updatedArsip.PenggunaUpdate = username
	// input_time = time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	// updatedArsip.TanggalUpdate = &input_time

	// if countUnsigned == 0 {
	// 	updatedArsip.IdStatusDokumen = 3
	// 	fmt.Println("update status arsip to Selesai")
	// } else {
	// 	fmt.Println("update status arsip to Diproses")
	// 	updatedArsip.IdStatusDokumen = 2
	// 	if req.NextIdTtdArsipDokumen != -1 {
	// 		pengguna, err := s.penggunaService.FindPenggunaById(nextTtd.IdPengguna)
	// 		if err != nil {
	// 			return err
	// 		}

	// 		nextSigner := response.ArsipDokumenResponse{NamaPenandatangan: pengguna.NamaLengkap, EmailPenandatangan: pengguna.Email}
	// 		err = ms.SignerMail(nextSigner, req.NamaJenisDokumen, ttd.PenggunaEntry, "/dokumen/detail/"+strconv.FormatUint(req.IdArsipDokumen, 10))
	// 		fmt.Println("email sent to: " + nextSigner.EmailPenandatangan)
	// 		if err != nil {
	// 			// tx.Rollback()
	// 			return err
	// 		}
	// 	}
	// }
	updatedArsip := entity.ArsipDokumen{}
	updatedArsip.Id = req.IdArsipDokumen
	updatedArsip.IdStatusDokumen = 5
	updatedArsip.PenggunaUpdate = username
	input_time = time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	updatedArsip.TanggalUpdate = &input_time
	err = s.arsipDokumenRepository.UpdateArsip(tx, updatedArsip)
	if err != nil {
		tx.Rollback()
		return err
	}

	// writing to log: set id_status_dokumen to 2 (diproses) or 3(selesai) of current arsip_dokumen
	if err = s.logService.WriteLogAktivitas(ctx, username, "Penandatangan", "Penandatanganan: update status arsip",
		arsip.ToString(), updatedArsip.ToString(), updatedArsip.TableName()); err != nil {
		// tx.Rollback()
		return err
	}

	tx.Commit()
	return err
}

func (s *arsipDokumenService) ConfirmReject(ctx *gin.Context, ms MailService, req request.PengajuRequest) error {
	tx := s.DB.Begin()
	ttd, err := s.arsipDokumenRepository.FindTtdArsipById(req.IdTtdArsipDokumen)
	if err != nil {
		return err
	}
	if ttd.FlagVisual {
		return errors.New("Penolakan tidak bisa dilakukan, terdapat visual gambar penandatangan pada dokumen. Silakan batalkan dokumen dan buat pengajuan baru.")
	}
	arsip, err := s.arsipDokumenRepository.FindArsipById(req.IdArsipDokumen)
	if err != nil {
		return err
	}
	jenisDokumen, err := s.arsipDokumenRepository.FindJenisDokumenByIdJDO(arsip.IdJenisDokumenOrganisasi)
	if err != nil {
		return err
	}
	username, _, err := util.ExtractUsernameListPeranByContext(ctx)
	if err != nil || !util.IsRequestAuthorizableByRole(ctx, "A02") {
		tx.Rollback()
		return errors.New("Unauthorized, Anda bukan Pengaju!")
	}
	updatedTtd := entity.TtdArsipDokumen{}
	false_flag := false
	updatedTtd.Id = ttd.Id
	updatedTtd.CurrentReject = &false_flag
	updatedTtd.FlagTtd = true
	updatedTtd.FlagUrutan = &false_flag
	updatedTtd.PenggunaUpdate = ttd.Pengguna.Username
	input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	fmt.Println("input_time ttd: ", input_time)
	updatedTtd.TanggalUpdate = &input_time
	fmt.Println("confirm reject ttdarsip: ", fmt.Sprintf("%+v", updatedTtd))
	err = s.arsipDokumenRepository.UpdateTtdArsip(tx, updatedTtd)
	if err != nil {
		tx.Rollback()
		return err
	}
	if err = s.logService.WriteLogAktivitas(ctx, username, "Pengaju", "Confirm Reject Dokumen",
		ttd.ToString(), updatedTtd.ToString(), ttd.TableName()); err != nil {
		return err
	}

	person, err := s.penggunaService.FindPenggunaByUsername(ttd.PenggunaEntry)
	if err != nil {
		return err
	}
	pengaju := response.PenggunaResponse{NamaLengkap: person.NamaLengkap, Email: person.Email}
	err = ms.NotifyRejectToPengaju(pengaju, jenisDokumen.Nama, username, "/dokumen/detail/"+strconv.FormatUint(req.IdArsipDokumen, 10))
	fmt.Println("email sent to: " + pengaju.Email)
	if err != nil {
		// tx.Rollback()
		return err
	}

	var nextTtd entity.TtdArsipDokumen
	if req.NextIdTtdArsipDokumen != -1 {
		updatedNextTtd := entity.TtdArsipDokumen{}
		updatedNextTtd.Id = uint64(req.NextIdTtdArsipDokumen)
		true_flag := true
		updatedNextTtd.FlagUrutan = &true_flag
		fmt.Println("next signer if exists: " + fmt.Sprintf("%+v", updatedNextTtd))
		err = s.arsipDokumenRepository.UpdateTtdArsip(tx, updatedNextTtd)
		if err != nil {
			tx.Rollback()
			return err
		}
		nextTtd, err = s.arsipDokumenRepository.FindTtdArsipById(uint64(req.NextIdTtdArsipDokumen))
		if err != nil {
			return err
		}
		if err = s.logService.WriteLogAktivitas(ctx, username, "Pengaju", "Pengaju: set flag_urutan true of next ttd",
			nextTtd.ToString(), updatedNextTtd.ToString(), updatedNextTtd.TableName()); err != nil {
			tx.Rollback()
			return err
		}

	}

	countUnsigned, err := s.arsipDokumenRepository.CountUnsignedArsip(int(req.IdArsipDokumen), int(req.IdTtdArsipDokumen))
	// unsignedTtd, err := s.arsipDokumenRepository.GetUnsignedArsip(int(req.IdArsipDokumen), int(req.IdTtdArsipDokumen))
	if err != nil {
		return err
	}

	fmt.Println("unsigned: ", countUnsigned)

	updatedArsip := entity.ArsipDokumen{}
	updatedArsip.Id = req.IdArsipDokumen
	updatedArsip.PenggunaUpdate = username
	// input_time = time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	fmt.Println("input_time arsip: ", input_time)
	updatedArsip.TanggalUpdate = &input_time

	if countUnsigned == 0 {
		updatedArsip.IdStatusDokumen = 3
		fmt.Println("update status arsip to Selesai")
	} else {
		fmt.Println("update status arsip to Diproses")
		updatedArsip.IdStatusDokumen = 2
		if req.NextIdTtdArsipDokumen != -1 {
			pengguna, err := s.penggunaService.FindPenggunaById(nextTtd.IdPengguna)
			if err != nil {
				return err
			}

			nextSigner := response.ArsipDokumenResponse{NamaPenandatangan: pengguna.NamaLengkap, EmailPenandatangan: pengguna.Email}
			err = ms.SignerMail(nextSigner, jenisDokumen.Nama, ttd.PenggunaEntry, "/dokumen/detail/"+strconv.FormatUint(req.IdArsipDokumen, 10))
			fmt.Println("email sent to: " + nextSigner.EmailPenandatangan)
			if err != nil {
				// tx.Rollback()
				return err
			}
		}
	}
	//Langsung atau jumlah penandatangan hanya 1
	if len(arsip.TtdArsipDokumen) == 1 {
		updatedArsip.IdStatusDokumen = 4
	}
	err = s.arsipDokumenRepository.UpdateArsip(tx, updatedArsip)
	if err != nil {
		tx.Rollback()
		return err
	}

	// writing to log: set id_status_dokumen to 2 (diproses) or 3(selesai) of current arsip_dokumen
	if err = s.logService.WriteLogAktivitas(ctx, username, "Pengaju", "Pengaju: update status arsip",
		arsip.ToString(), updatedArsip.ToString(), updatedArsip.TableName()); err != nil {
		// tx.Rollback()
		return err
	}

	tx.Commit()
	return err
}

// func (s *arsipDokumenService) DenyReject(ctx *gin.Context, ms MailService, id_ttd_arsip_dokumen uint64) error {
func (s *arsipDokumenService) DenyReject(ctx *gin.Context, ms MailService, req request.DenyRejectRequest) error {
	tx := s.DB.Begin()
	id_ttd_arsip_dokumen := req.IdTtdArsipDokumen
	fmt.Println("id: ", id_ttd_arsip_dokumen)
	ttd, err := s.arsipDokumenRepository.FindTtdArsipById(id_ttd_arsip_dokumen)
	if err != nil {
		return err
	}

	arsip, err := s.arsipDokumenRepository.FindArsipById(ttd.IdArsipDokumen)
	if err != nil {
		tx.Rollback()
		return err
	}

	jenisDokumen, err := s.arsipDokumenRepository.FindJenisDokumenByIdJDO(arsip.IdJenisDokumenOrganisasi)
	if err != nil {
		tx.Rollback()
		return err
	}

	username, _, err := util.ExtractUsernameListPeranByContext(ctx)
	if err != nil || !util.IsRequestAuthorizableByRole(ctx, "A02") {
		tx.Rollback()
		return errors.New("Unauthorized, Anda bukan Pengaju!")
	}
	updatedTtd := entity.TtdArsipDokumen{}
	false_flag := false
	updatedTtd.Id = ttd.Id
	empty_string := ""
	updatedTtd.Keterangan = &empty_string
	updatedTtd.CurrentReject = &false_flag
	updatedTtd.KeteranganSanggahTolak = &req.KeteranganSanggahTolak
	updatedTtd.PenggunaUpdate = ttd.Pengguna.Username
	input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	updatedTtd.TanggalUpdate = &input_time
	fmt.Println("deny reject ttdarsip: ", fmt.Sprintf("%+v", updatedTtd))
	err = s.arsipDokumenRepository.UpdateTtdArsip(tx, updatedTtd)
	if err != nil {
		tx.Rollback()
		return err
	}
	if err = s.logService.WriteLogAktivitas(ctx, username, "Pengaju", "Deny Reject Dokumen",
		ttd.ToString(), updatedTtd.ToString(), ttd.TableName()); err != nil {
		return err
	}

	person, err := s.penggunaService.FindPenggunaById(ttd.IdPengguna)
	if err != nil {
		return err
	}
	err = ms.NotifyDenyRejectToPenandatangan(person, jenisDokumen.Nama, username, "/dokumen/detail/"+strconv.FormatUint(ttd.IdArsipDokumen, 10))
	fmt.Println("email sent to: " + person.Email)
	if err != nil {
		// tx.Rollback()
		return err
	}

	updatedArsip := entity.ArsipDokumen{}
	updatedArsip.Id = ttd.IdArsipDokumen
	updatedArsip.IdStatusDokumen = 2
	updatedArsip.PenggunaUpdate = username
	// input_time = time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	updatedArsip.TanggalUpdate = &input_time
	err = s.arsipDokumenRepository.UpdateArsip(tx, updatedArsip)
	if err != nil {
		tx.Rollback()
		return err
	}

	if err = s.logService.WriteLogAktivitas(ctx, username, "Pengaju", "Pengaju: update status arsip",
		arsip.ToString(), updatedArsip.ToString(), updatedArsip.TableName()); err != nil {
		return err
	}

	tx.Commit()
	return err
}

func (s *arsipDokumenService) UpdateTime(ctx *gin.Context, id_ttd_arsip_dokumen uint64) error {
	tx := s.DB.Begin()
	ttd, err := s.arsipDokumenRepository.FindTtdArsipById(id_ttd_arsip_dokumen)
	if err != nil {
		return err
	}

	username, _, err := util.ExtractUsernameListPeranByContext(ctx)
	if err != nil || !util.IsRequestAuthorizableByRole(ctx, "A03") {
		tx.Rollback()
		return errors.New("Unauthorized, Anda bukan Penandatangan!")
	}
	updatedTtd := entity.TtdArsipDokumen{}
	updatedTtd.Id = id_ttd_arsip_dokumen
	updatedTtd.PenggunaUpdate = username
	input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	updatedTtd.TanggalUpdate = &input_time
	fmt.Println("update time (remove notification) ttdarsip: ", fmt.Sprintf("%+v", updatedTtd))
	err = s.arsipDokumenRepository.UpdateTtdArsip(tx, updatedTtd)
	if err != nil {
		tx.Rollback()
		return err
	}
	if err = s.logService.WriteLogAktivitas(ctx, username, "Penandatangan", "Update time (Remove notification)",
		ttd.ToString(), updatedTtd.ToString(), ttd.TableName()); err != nil {
		return err
	}

	tx.Commit()
	return err
}

func (s *arsipDokumenService) SetNonaktif(ctx *gin.Context, req request.NonaktifArsipRequest) error {
	tx := s.DB.Begin()
	arsip, err := s.arsipDokumenRepository.FindArsipById(req.Id)
	if err != nil {
		return err
	}

	username, _, err := util.ExtractUsernameListPeranByContext(ctx)
	if err != nil || !util.IsRequestAuthorizableByRole(ctx, "A01") {
		tx.Rollback()
		return errors.New("Unauthorized, Anda bukan Admin!")
	}
	updatedArsip := entity.ArsipDokumen{}
	updatedArsip.Id = req.Id
	updatedArsip.PenggunaUpdate = username
	input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
	updatedArsip.TanggalUpdate = &input_time
	updatedArsip.KeteranganFlagAktif = req.KeteranganFlagAktif
	flag_false := false
	updatedArsip.FlagAktif = &flag_false
	fmt.Println("nonaktif arsip: ", fmt.Sprintf("%+v", updatedArsip))
	err = s.arsipDokumenRepository.UpdateArsip(tx, updatedArsip)
	if err != nil {
		tx.Rollback()
		return err
	}
	if err = s.logService.WriteLogAktivitas(ctx, username, "Admin", "Nonaktifkan Arsip",
		arsip.ToString(), updatedArsip.ToString(), arsip.TableName()); err != nil {
		return err
	}

	tx.Commit()
	return err
}

func (s *arsipDokumenService) GetBsreVariable(ctx *gin.Context) (util.BSRE, error) {
	var bsre util.BSRE
	url, err := s.pengaturanService.GetPengaturanByField(ctx, "system", "variable", "API_BSRE_URL")
	if err != nil {
		return bsre, err
	}
	username, err := s.pengaturanService.GetPengaturanByField(ctx, "system", "variable", "API_BSRE_USERNAME")
	if err != nil {
		return bsre, err
	}
	password, err := s.pengaturanService.GetPengaturanByField(ctx, "system", "variable", "API_BSRE_PASSWORD")
	if err != nil {
		return bsre, err
	}
	bsre = util.BSRE{Url: url.Value, Username: username.Value, Password: password.Value}
	return bsre, err
}

func (s *arsipDokumenService) ApiBsreGetUserStatus() (string, string, error) {
	tx := s.DB.Begin()
	nik := os.Getenv("API_BSRE_NIK_SAMPLE")
	if nik == "" {
		nik = "0803202100007062"
	}
	//get user status
	res, req, err := util.GetUserStatus(nik)
	if err != nil {
		return "", "", err
	}

	tx.Commit()
	return res, req, err
}

func (s *arsipDokumenService) ApiBsreSignTest(path_file string, id_user_login uint64, passphrase string) ([]byte, string, error) {
	tx := s.DB.Begin()
	pengguna, _, _, err := s.penggunaService.FindPenggunaByIdComplete(id_user_login)
	if err != nil {
		return []byte{}, "", err
	}
	// if passphrase != "" {
	// 	if pengguna.Passphrase != passphrase {
	// 		tx.Rollback()
	// 		return []byte{}, "", errors.New("Passphrase tidak cocok")
	// 	}
	// }

	// sign invisible
	bsre, err := s.GetBsreVariable(nil)
	if err != nil {
		return []byte{}, "", err
	}
	res, id_dokumen_bsre, err := util.SignInvisible(path_file, pengguna, passphrase, bsre)
	if err != nil {
		tx.Rollback()
		return []byte{}, "", err
	}

	fmt.Println("id_dokumen_bsre: ", id_dokumen_bsre)

	// if !strings.Contains(status, "200") {
	// 	tx.Rollback()
	// 	return []byte{}, "", errors.New("Error with status: " + status)
	// }

	// tx.Commit()
	return res, id_dokumen_bsre, err
}

func (s *arsipDokumenService) ApiBsreSign(ctx *gin.Context, tx *gorm.DB, path_file string, id_user_login uint64, passphrase string) ([]byte, string, error) {
	// tx := s.DB.Begin()
	pengguna, _, _, err := s.penggunaService.FindPenggunaByIdComplete(id_user_login)
	if err != nil {
		tx.Rollback()
		return []byte{}, "", err
	}
	if passphrase == "" {
		tx.Rollback()
		return []byte{}, "", errors.New("Passphrase kosong")
	}

	// sign invisible
	bsre, err := s.GetBsreVariable(nil)
	if err != nil {
		return []byte{}, "", err
	}
	res, id_dokumen_bsre, err := util.SignInvisible(path_file, pengguna, passphrase, bsre)
	if err != nil {
		tx.Rollback()
		s.logEsignService.WriteLogEsign(ctx, "Pengguna dengan username: "+pengguna.Username+" gagal melakukan penandatanganan dokumen. Pesan: "+err.Error())
		return []byte{}, "", err
	}

	fmt.Println("id_dokumen_bsre: ", id_dokumen_bsre)

	// if !strings.Contains(status, "200") {
	// 	tx.Rollback()
	// 	return []byte{}, "", errors.New("Error with status: " + status)
	// }

	// tx.Commit()
	return res, id_dokumen_bsre, err
}

// func (s *arsipDokumenService) ApiBsreSignFooter(tx *gorm.DB, path_file string, id_user_login uint64, passphrase string) ([]byte, string, error) {
// 	pengguna, _, err := s.penggunaService.FindPenggunaByIdComplete(id_user_login)
// 	if err != nil {
// 		tx.Rollback()
// 		return []byte{}, "", err
// 	}
// 	if passphrase != "" {
// 		if pengguna.Passphrase != passphrase {
// 			tx.Rollback()
// 			return []byte{}, "", errors.New("Passphrase tidak cocok")
// 		}
// 	}
// 	image_ttd_path := "files/stamp/deskripsi.jpg"
// 	// sign with footer
// 	res64, status, err := util.SignWithImageTTD(path_file, image_ttd_path, pengguna, passphrase)
// 	if err != nil {
// 		tx.Rollback()
// 		return []byte{}, "", err
// 	}
// 	//getting binary
// 	res, err := base64.StdEncoding.DecodeString(res64)
// 	if err != nil {
// 		tx.Rollback()
// 		return []byte{}, "", err
// 	}
// 	//generate random id
// 	id_dokumen_bsre, err := password.Generate(32, 2, 0, false, false)
// 	if err != nil {
// 		return []byte{}, "", err
// 	}

// 	fmt.Println("id_dokumen_bsre: ", id_dokumen_bsre)

// 	if !strings.Contains(status, "200") {
// 		tx.Rollback()
// 		return []byte{}, "", errors.New("Error with status: " + status)
// 	}

// 	// tx.Commit()
// 	return res, id_dokumen_bsre, err
// }

// func (s *arsipDokumenService) ApiBsreSignWithImage( /*tx *gorm.DB, */ id_arsip uint64, id_ttd_arsip_dokumen uint64, id_user_login uint64) (string, error) {
// 	tx := s.DB.Begin()
// 	// nik := os.Getenv("API_BSRE_NIK_SAMPLE")
// 	// passphrase := os.Getenv("API_BSRE_PASSPHRASE_SAMPLE")

// 	status := "fail"
// 	fmt.Println("nik", os.Getenv("API_BSRE_NIK_SAMPLE"))
// 	fmt.Println("passphrase", os.Getenv("API_BSRE_PASSPHRASE_SAMPLE"))

// 	//sign with imageTTD
// 	pengguna, _, err := s.penggunaService.FindPenggunaByIdComplete(id_user_login)
// 	if err != nil {
// 		tx.Rollback()
// 		return "", err
// 	}
// 	ttd, err := s.arsipDokumenRepository.FindTtdArsipById(id_ttd_arsip_dokumen)
// 	if err != nil {
// 		tx.Rollback()
// 		return "", err
// 	}
// 	arsip, err := s.arsipDokumenRepository.FindArsipById(id_arsip)
// 	if err != nil {
// 		tx.Rollback()
// 		return "", err
// 	}

// 	// nik = person.Nik
// 	// passphrase = person.Passphrase
// 	imageTTDPath := ""
// 	if ttd.TipeTtd == 1 {
// 		imageTTDPath = filepath.Join(util.BASE_PATH, util.SIGNIMG_PATH, pengguna.FileParaf)
// 	} else if ttd.TipeTtd == 2 {
// 		imageTTDPath = filepath.Join(util.BASE_PATH, util.SIGNIMG_PATH, pengguna.FileTtd)
// 	} else {
// 		tx.Rollback()
// 		return "", errors.New("tipe tanda tangan tidak dikenal")
// 	}

// 	res, status, err := util.SignWithImageTTD(arsip.PathFile, imageTTDPath, pengguna, pengguna.Passphrase)
// 	if err != nil {
// 		tx.Rollback()
// 		return "", err
// 	}

// 	fmt.Println("len(res): ", len(res))

// 	if strings.Contains(status, "200") {
// 		err = util.DeleteFileByFilePath(arsip.PathFile)
// 		if err != nil {
// 			return "", err
// 		}
// 		randomString, err := password.Generate(10, 2, 0, false, false)
// 		if err != nil {
// 			log.Println(err)
// 		}
// 		filename := strconv.FormatUint(id_arsip, 10) + "_" + randomString + ".pdf"
// 		targetPath := filepath.Join(util.BASE_PATH, util.DOCS_PATH, filename)

// 		fmt.Println("saving file from bsre to: " + targetPath)

// 		bytes, err := base64.StdEncoding.DecodeString(res)
// 		if err != nil {
// 			tx.Rollback()
// 			return "", err
// 		}

// 		// f, err := os.Create(targetPath)
// 		// if err != nil {
// 		// 	tx.Rollback()
// 		// 	return "", err
// 		// }
// 		// defer f.Close()
// 		// if _, err := f.Write(bytes); err != nil {
// 		// 	tx.Rollback()
// 		// 	return "", err
// 		// }
// 		// if err := f.Sync(); err != nil {
// 		// 	tx.Rollback()
// 		// 	return "", err
// 		// }
// 		err = os.WriteFile(targetPath, bytes, 0644)
// 		if err != nil {
// 			tx.Rollback()
// 			fmt.Println("Error writing PDF file:", err)
// 			return "", err
// 		}
// 		updatedArsip := entity.ArsipDokumen{}
// 		updatedArsip.Id = id_arsip
// 		updatedArsip.FileDokumen = filename
// 		updatedArsip.PathFile = targetPath
// 		input_time := time.Now().In(time.FixedZone("UTC+7", 7*60*60))
// 		updatedArsip.TanggalUpdate = &input_time
// 		err = s.arsipDokumenRepository.UpdateArsip(tx, updatedArsip)
// 		if err != nil {
// 			tx.Rollback()
// 			return "", err
// 		}
// 	}

// 	tx.Commit()
// 	return status, err
// }

func (s *arsipDokumenService) requestSetup(pengaturanEksternal *entity.PengaturanEksternal, arsipDokumen entity.ArsipDokumen, signArsipDokumenRequest request.SignArsipDokumenRequest, filename string, result_file []byte) (*http.Request, error) {
	var authConfig, headerConfig, bodyConfig map[string]string

	authConfigBytes := []byte(*pengaturanEksternal.AuthConfig)
	err := json.Unmarshal(authConfigBytes, &authConfig)
	if err != nil {
		log.Printf("Error unmarshalling AuthConfig: %v", err)
	}

	headerConfigBytes := []byte(*pengaturanEksternal.HeaderConfig)
	err = json.Unmarshal(headerConfigBytes, &headerConfig)
	if err != nil {
		log.Printf("Error unmarshalling HeaderConfig: %v", err)
	}

	bodyConfigBytes := []byte(*pengaturanEksternal.BodyConfig)
	err = json.Unmarshal(bodyConfigBytes, &bodyConfig)
	if err != nil {
		log.Printf("Error unmarshalling BodyConfig: %v", err)
	}

	var requestBody bytes.Buffer
	writer := multipart.NewWriter(&requestBody)
	fileWriter, err := writer.CreateFormFile("file", filename)

	if err != nil {
		return nil, err
	}

	_, err = fileWriter.Write(result_file)
	if err != nil {
		return nil, err
	}

	if len(bodyConfig) > 0 {
		var requestValue = reflect.ValueOf(signArsipDokumenRequest)
		var entityValue = reflect.ValueOf(arsipDokumen)

		for key, value := range bodyConfig {
			parts := strings.Split(value, ".")
			objectName := parts[0]
			structname := parts[1]
			fieldName := parts[2]

			var fieldValue string
			if objectName == "request" {
				field := requestValue.FieldByName(fieldName)
				if field.IsValid() {
					fieldValue = fmt.Sprintf("%v", field.Interface())
				} else {
					return nil, fmt.Errorf("field %s does not exist in request %s", fieldName, structname)
				}
			} else if objectName == "entity" {
				field := entityValue.FieldByName(fieldName)
				if field.IsValid() {
					fieldValue = fmt.Sprintf("%v", field.Interface())
				} else {
					return nil, fmt.Errorf("field %s does not exist in entity %s", fieldName, structname)
				}
			}

			err = writer.WriteField(key, fieldValue)
			if err != nil {
				return nil, err
			}
		}
	}

	err = writer.Close()
	if err != nil {
		return nil, err
	}

	request, err := http.NewRequest("POST", pengaturanEksternal.CallBack, &requestBody)
	if err != nil {
		return nil, err
	}

	if len(headerConfig) > 0 {
		for key, value := range headerConfig {
			request.Header.Set(key, value)
		}
	}

	if len(authConfig) > 0 {
		if authConfig["type"] == "basic" {
			request.SetBasicAuth(authConfig["username"], authConfig["password"])
		} else if authConfig["type"] == "bearer" {
			request.Header.Add("Authorization", "Bearer "+authConfig["token"])
		}
	}

	request.Header.Set("Content-Type", writer.FormDataContentType())

	log.Println("Request created")
	log.Println("Request headers setup result:", request.Header)
	bodyBytes, err := io.ReadAll(request.Body)
	if err != nil {
		fmt.Println("Error reading request body:", err)
		return nil, err
	} else {
		fmt.Println("Request body setup result:", string(bodyBytes))
		request.Body = io.NopCloser(bytes.NewBuffer(bodyBytes))
	}

	return request, nil
}
