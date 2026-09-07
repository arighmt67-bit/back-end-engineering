/* Middleware unggah berkas menggunakan multer.
   Menerapkan dua validasi wajib: ukuran maksimal 5 MB dan MIME type application/pdf. */
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const { nanoid } = require('nanoid');
const { InvariantError } = require('../exceptions/errors');

const DIREKTORI = path.resolve(__dirname, '..', '..', 'uploads');
const UKURAN_MAKSIMAL = 5 * 1024 * 1024; // 5 MB

/* Direktori penyimpanan dibuat otomatis bila belum ada. */
if (!fs.existsSync(DIREKTORI)) {
  fs.mkdirSync(DIREKTORI, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, DIREKTORI),
  filename: (_req, file, cb) => {
    const ekstensi = path.extname(file.originalname) || '.pdf';
    cb(null, `document-${nanoid(16)}${ekstensi}`);
  },
});

/* Hanya berkas PDF yang diterima. Berkas lain ditolak sebelum tersimpan ke disk. */
const fileFilter = (_req, file, cb) => {
  if (file.mimetype !== 'application/pdf') {
    return cb(new InvariantError('File is required and must be a PDF document'), false);
  }
  return cb(null, true);
};

const multerUpload = multer({
  storage,
  fileFilter,
  limits: { fileSize: UKURAN_MAKSIMAL },
}).single('document');

/* Pembungkus agar seluruh galat multer dikembalikan sebagai 400 lewat error handler terpusat. */
const upload = (req, res, next) => multerUpload(req, res, (err) => {
  if (!err) return next();

  if (err instanceof multer.MulterError) {
    if (err.code === 'LIMIT_FILE_SIZE') {
      return next(new InvariantError('Ukuran berkas melebihi batas maksimal 5 MB'));
    }
    return next(new InvariantError(`Berkas tidak valid: ${err.message}`));
  }

  /* Galat penguraian multipart (mis. boundary rusak) tetap kesalahan klien, bukan server. */
  if (err instanceof InvariantError) {
    return next(err);
  }

  return next(new InvariantError('File is required and must be a PDF document'));
});

module.exports = upload;
