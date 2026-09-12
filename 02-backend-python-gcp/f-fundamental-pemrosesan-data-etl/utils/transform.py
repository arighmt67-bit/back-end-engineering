import pandas as pd
import re

EXCHANGE_RATE = 16000.0

def clean_price(val):
    """
    Konversi string harga '$xxx.xx' menjadi float IDR (kurs Rp16.000).
    Jika 'Price Unavailable', None, atau format tidak valid -> return None.
    """
    if pd.isna(val):
        return None
    val_str = str(val).strip()
    if val_str == "Price Unavailable" or "Unavailable" in val_str:
        return None
    match = re.search(r'\$?([0-9]+(?:\.[0-9]+)?)', val_str)
    if match:
        try:
            usd = float(match.group(1))
            return float(usd * EXCHANGE_RATE)
        except (ValueError, TypeError):
            return None
    return None

def clean_rating(val):
    """
    Ekstrak rating angka float dari 'Rating: ⭐ 3.9 / 5'.
    Jika 'Invalid Rating' atau format tidak valid -> return None.
    """
    if pd.isna(val):
        return None
    val_str = str(val).strip()
    if "Invalid Rating" in val_str or "Not Rated" in val_str:
        return None
    match = re.search(r'([0-9]+(?:\.[0-9]+)?)\s*/\s*5', val_str)
    if match:
        try:
            return float(match.group(1))
        except (ValueError, TypeError):
            return None
    # Coba format desimal langsung
    match_num = re.search(r'([0-9]+(?:\.[0-9]+)?)', val_str)
    if match_num:
        try:
            return float(match_num.group(1))
        except (ValueError, TypeError):
            return None
    return None

def clean_colors(val):
    """
    Ekstrak integer jumlah warna dari '3 Colors' atau '5 Colors'.
    """
    if pd.isna(val):
        return None
    val_str = str(val).strip()
    match = re.search(r'(\d+)', val_str)
    if match:
        try:
            return int(match.group(1))
        except (ValueError, TypeError):
            return None
    return None

def clean_size(val):
    """
    Bersihkan teks ukuran dari 'Size: M' -> 'M'.
    """
    if pd.isna(val):
        return None
    val_str = str(val).strip()
    val_str = re.sub(r'^Size:\s*', '', val_str, flags=re.IGNORECASE).strip()
    return val_str if val_str else None

def clean_gender(val):
    """
    Bersihkan teks gender dari 'Gender: Men' -> 'Men'.
    """
    if pd.isna(val):
        return None
    val_str = str(val).strip()
    val_str = re.sub(r'^Gender:\s*', '', val_str, flags=re.IGNORECASE).strip()
    return val_str if val_str else None

def transform_data(raw_data):
    """
    Melakukan data cleansing & transformation:
    1. Filter out 'Unknown Product'
    2. Convert Price to float IDR (kurs 16.000)
    3. Convert Rating to float
    4. Convert Colors to int
    5. Clean Size & Gender strings
    6. Drop duplicates
    7. Drop nulls
    8. Enforce dtypes:
       - Title: object
       - Price: float64
       - Rating: float64
       - Colors: int64
       - Size: object
       - Gender: object
       - timestamp: object (jika ada)
    """
    try:
        if raw_data is None:
            raise ValueError("Input data cannot be None")

        if isinstance(raw_data, list):
            df = pd.DataFrame(raw_data)
        elif isinstance(raw_data, pd.DataFrame):
            df = raw_data.copy()
        else:
            raise TypeError("Expected raw_data to be a list of dicts or pandas DataFrame")

        if df.empty:
            return pd.DataFrame(columns=['Title', 'Price', 'Rating', 'Colors', 'Size', 'Gender', 'timestamp'])

        # 1. Filter invalid Title (seperti 'Unknown Product')
        if 'Title' in df.columns:
            df['Title'] = df['Title'].astype(str).str.strip()
            df = df[~df['Title'].isin(['Unknown Product', 'None', 'nan', ''])]

        # 2. Bersihkan kolom Price
        if 'Price' in df.columns:
            df['Price'] = df['Price'].apply(clean_price)

        # 3. Bersihkan kolom Rating
        if 'Rating' in df.columns:
            df['Rating'] = df['Rating'].apply(clean_rating)

        # 4. Bersihkan kolom Colors
        if 'Colors' in df.columns:
            df['Colors'] = df['Colors'].apply(clean_colors)

        # 5. Bersihkan kolom Size
        if 'Size' in df.columns:
            df['Size'] = df['Size'].apply(clean_size)

        # 6. Bersihkan kolom Gender
        if 'Gender' in df.columns:
            df['Gender'] = df['Gender'].apply(clean_gender)

        # 7. Drop missing values (null / NaN)
        df = df.dropna()

        # 8. Drop duplicates
        # Duplikat dicek berdasarkan Title atau kombinasi produk
        df = df.drop_duplicates(subset=['Title'])

        if df.empty:
            return pd.DataFrame(columns=['Title', 'Price', 'Rating', 'Colors', 'Size', 'Gender', 'timestamp'])

        # 9. Pastikan tipe data sesuai spesifikasi rubrik:
        # Title: object, Price: float64, Rating: float64, Colors: int64, Size: object, Gender: object
        df['Title'] = df['Title'].astype('object')
        df['Price'] = df['Price'].astype('float64')
        df['Rating'] = df['Rating'].astype('float64')
        df['Colors'] = df['Colors'].astype('int64')
        df['Size'] = df['Size'].astype('object')
        df['Gender'] = df['Gender'].astype('object')
        if 'timestamp' in df.columns:
            df['timestamp'] = df['timestamp'].astype('object')

        # Reset index agar rapi
        df = df.reset_index(drop=True)

        return df

    except Exception as e:
        print(f"An error occurred during transformation: {e}")
        return None
