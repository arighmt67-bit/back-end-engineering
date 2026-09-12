import sys
import os
from utils.extract import scrape_main
from utils.transform import transform_data
from utils.load import load_to_csv, load_to_postgresql, load_to_google_sheets

# Konfigurasi PostgreSQL lokal
DB_URI = os.getenv('DB_URI', 'postgresql://arirahmatr@localhost:5432/dicoding_pemda')

# Konfigurasi Google Sheets (Optional via environment / fallback)
GOOGLE_SHEETS_ID = os.getenv('GOOGLE_SHEETS_ID', '')
CREDENTIALS_PATH = os.getenv('CREDENTIALS_PATH', 'google-sheets-api.json')

def run_pipeline():
    print("=== [1/3] Memulai Tahap Extract ===")
    raw_data = scrape_main(total_pages=50)
    if not raw_data:
        print("Gagal mengekstrak data dari Fashion Studio.")
        sys.exit(1)
    print(f"Berhasil mengekstrak {len(raw_data)} data mentah.")

    print("\n=== [2/3] Memulai Tahap Transform ===")
    cleaned_df = transform_data(raw_data)
    if cleaned_df is None or cleaned_df.empty:
        print("Gagal membersihkan data atau DataFrame kosong.")
        sys.exit(1)
    print(f"Berhasil membersihkan data. Total produk valid: {len(cleaned_df)} baris.")
    print("Ringkasan data bersih:")
    print(cleaned_df.info())
    print("\nContoh 5 data teratas:")
    print(cleaned_df.head())

    print("\n=== [3/3] Memulai Tahap Load ===")
    # 1. Flat File CSV
    load_to_csv(cleaned_df, 'products.csv')

    # 2. PostgreSQL
    load_to_postgresql(cleaned_df, connection_uri=DB_URI)

    # 3. Google Sheets (jika Google Sheets ID disediakan)
    if GOOGLE_SHEETS_ID and os.path.exists(CREDENTIALS_PATH):
        load_to_google_sheets(cleaned_df, spreadsheet_id=GOOGLE_SHEETS_ID, credentials_path=CREDENTIALS_PATH)
    else:
        print("Google Sheets ID tidak disetel, lewati load ke Google Sheets (Kriteria 2 terpenuhi via CSV + PostgreSQL).")

    print("\n=== ETL Pipeline Selesai dengan Sukses! ===")

if __name__ == '__main__':
    run_pipeline()
