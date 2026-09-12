import os
import pandas as pd
from sqlalchemy import create_engine

def load_to_csv(df, file_path='products.csv'):
    """
    Menyimpan DataFrame ke dalam berkas flat file .CSV.
    """
    try:
        if df is None or not isinstance(df, pd.DataFrame):
            raise ValueError("Input must be a valid pandas DataFrame")

        df.to_csv(file_path, index=False)
        print(f"Data successfully saved to CSV: {file_path}")
        return True
    except Exception as e:
        print(f"Failed to save data to CSV: {e}")
        return False

def load_to_postgresql(df, connection_uri='postgresql://arirahmatr@localhost:5432/dicoding_pemda', table_name='products'):
    """
    Menyimpan DataFrame ke dalam database relasional PostgreSQL menggunakan SQLAlchemy.
    """
    try:
        if df is None or not isinstance(df, pd.DataFrame):
            raise ValueError("Input must be a valid pandas DataFrame")

        engine = create_engine(connection_uri)
        df.to_sql(table_name, engine, if_exists='replace', index=False)
        print(f"Data successfully loaded to PostgreSQL table '{table_name}'")
        return True
    except Exception as e:
        print(f"Failed to load data to PostgreSQL: {e}")
        return False

def load_to_google_sheets(df, spreadsheet_id, credentials_path='google-sheets-api.json', range_name='Sheet1!A1'):
    """
    Menyimpan DataFrame ke dalam Google Sheets API.
    Membutuhkan berkas service account credentials JSON.
    """
    try:
        if df is None or not isinstance(df, pd.DataFrame):
            raise ValueError("Input must be a valid pandas DataFrame")

        if not os.path.exists(credentials_path):
            raise FileNotFoundError(f"Credentials file not found at: {credentials_path}")

        from google.oauth2 import service_account
        from googleapiclient.discovery import build

        scopes = ['https://www.googleapis.com/auth/spreadsheets']
        creds = service_account.Credentials.from_service_account_file(
            credentials_path, scopes=scopes
        )

        service = build('sheets', 'v4', credentials=creds)

        # Siapkan data: baris pertama header, diikuti baris-baris data
        values = [df.columns.tolist()] + df.astype(str).values.tolist()
        body = {'values': values}

        # Clear existing data first
        service.spreadsheets().values().clear(
            spreadsheetId=spreadsheet_id,
            range='Sheet1'
        ).execute()

        # Update values
        result = service.spreadsheets().values().update(
            spreadsheetId=spreadsheet_id,
            range=range_name,
            valueInputOption='USER_ENTERED',
            body=body
        ).execute()

        print(f"Data successfully loaded to Google Sheets: {result.get('updatedCells')} cells updated.")
        return True
    except Exception as e:
        print(f"Failed to load data to Google Sheets: {e}")
        return False
