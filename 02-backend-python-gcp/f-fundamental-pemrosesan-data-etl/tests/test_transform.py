import pytest
import pandas as pd
import numpy as np
from utils.transform import (
    clean_price,
    clean_rating,
    clean_colors,
    clean_size,
    clean_gender,
    transform_data,
    EXCHANGE_RATE
)

def test_clean_price():
    assert clean_price("$100.00") == 100.0 * 16000.0
    assert clean_price("102.15") == 102.15 * 16000.0
    assert clean_price("Price Unavailable") is None
    assert clean_price(None) is None
    assert clean_price("invalid text") is None

def test_clean_rating():
    assert clean_rating("Rating: ⭐ 3.9 / 5") == 3.9
    assert clean_rating("Rating: ⭐ 4.8 / 5") == 4.8
    assert clean_rating("Rating: ⭐ Invalid Rating / 5") is None
    assert clean_rating("Not Rated") is None
    assert clean_rating(None) is None
    assert clean_rating("5.0") == 5.0

def test_clean_colors():
    assert clean_colors("3 Colors") == 3
    assert clean_colors("5 Colors") == 5
    assert clean_colors("Color: 1") == 1
    assert clean_colors("No colors") is None
    assert clean_colors(None) is None

def test_clean_size():
    assert clean_size("Size: M") == "M"
    assert clean_size("Size: XXL") == "XXL"
    assert clean_size("XL") == "XL"
    assert clean_size("") is None
    assert clean_size(None) is None

def test_clean_gender():
    assert clean_gender("Gender: Men") == "Men"
    assert clean_gender("Gender: Women") == "Women"
    assert clean_gender("Gender: Unisex") == "Unisex"
    assert clean_gender("Kids") == "Kids"
    assert clean_gender(None) is None

def test_transform_data_success():
    raw_data = [
        {
            'Title': 'T-shirt 2',
            'Price': '$102.15',
            'Rating': 'Rating: ⭐ 3.9 / 5',
            'Colors': '3 Colors',
            'Size': 'Size: M',
            'Gender': 'Gender: Women',
            'timestamp': '2026-09-12T10:00:00'
        },
        {
            'Title': 'Unknown Product',
            'Price': '$100.00',
            'Rating': 'Rating: ⭐ Invalid Rating / 5',
            'Colors': '5 Colors',
            'Size': 'Size: M',
            'Gender': 'Gender: Men',
            'timestamp': '2026-09-12T10:00:00'
        },
        {
            'Title': 'T-shirt 2',  # Duplicate
            'Price': '$102.15',
            'Rating': 'Rating: ⭐ 3.9 / 5',
            'Colors': '3 Colors',
            'Size': 'Size: M',
            'Gender': 'Gender: Women',
            'timestamp': '2026-09-12T10:00:00'
        },
        {
            'Title': 'Hoodie 3',
            'Price': 'Price Unavailable',  # Null price
            'Rating': 'Rating: ⭐ 4.8 / 5',
            'Colors': '3 Colors',
            'Size': 'Size: L',
            'Gender': 'Gender: Unisex',
            'timestamp': '2026-09-12T10:00:00'
        }
    ]

    df = transform_data(raw_data)
    assert df is not None
    assert isinstance(df, pd.DataFrame)
    assert len(df) == 1
    row = df.iloc[0]
    assert row['Title'] == 'T-shirt 2'
    assert row['Price'] == 102.15 * 16000.0
    assert row['Rating'] == 3.9
    assert row['Colors'] == 3
    assert row['Size'] == 'M'
    assert row['Gender'] == 'Women'

    # Check dtypes
    assert df['Title'].dtype == 'object'
    assert df['Price'].dtype == 'float64'
    assert df['Rating'].dtype == 'float64'
    assert df['Colors'].dtype == 'int64'
    assert df['Size'].dtype == 'object'
    assert df['Gender'].dtype == 'object'

def test_transform_data_dataframe_input():
    raw_df = pd.DataFrame([{
        'Title': 'Pants 4',
        'Price': '$467.31',
        'Rating': 'Rating: ⭐ 3.3 / 5',
        'Colors': '3 Colors',
        'Size': 'Size: XL',
        'Gender': 'Gender: Men',
        'timestamp': '2026-09-12T10:00:00'
    }])
    df = transform_data(raw_df)
    assert len(df) == 1
    assert df.iloc[0]['Title'] == 'Pants 4'

def test_transform_data_empty():
    df = transform_data([])
    assert df.empty

def test_transform_data_invalid_input():
    assert transform_data(None) is None
    assert transform_data(12345) is None

def test_transform_data_all_dropped():
    raw_data = [
        {
            'Title': 'Unknown Product',
            'Price': '$100.00',
            'Rating': 'Rating: ⭐ Invalid Rating / 5',
            'Colors': '5 Colors',
            'Size': 'Size: M',
            'Gender': 'Gender: Men'
        }
    ]
    df = transform_data(raw_data)
    assert df.empty
