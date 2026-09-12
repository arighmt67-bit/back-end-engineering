import pytest
import os
import pandas as pd
from unittest.mock import patch, MagicMock
from utils.load import load_to_csv, load_to_postgresql, load_to_google_sheets

@pytest.fixture
def sample_df():
    return pd.DataFrame([
        {
            'Title': 'T-shirt 2',
            'Price': 1634400.0,
            'Rating': 3.9,
            'Colors': 3,
            'Size': 'M',
            'Gender': 'Women',
            'timestamp': '2026-09-12T10:00:00'
        }
    ])

def test_load_to_csv_success(sample_df, tmp_path):
    output_file = tmp_path / "test_products.csv"
    res = load_to_csv(sample_df, str(output_file))
    assert res is True
    assert os.path.exists(output_file)
    read_back = pd.read_csv(output_file)
    assert len(read_back) == 1
    assert read_back.iloc[0]['Title'] == 'T-shirt 2'

def test_load_to_csv_invalid(tmp_path):
    assert load_to_csv(None) is False
    assert load_to_csv("not_a_df") is False

@patch('utils.load.create_engine')
def test_load_to_postgresql_success(mock_create_engine, sample_df):
    mock_engine = MagicMock()
    mock_create_engine.return_value = mock_engine

    with patch.object(pd.DataFrame, 'to_sql') as mock_to_sql:
        res = load_to_postgresql(sample_df, connection_uri='postgresql://test:test@localhost:5432/test')
        assert res is True
        mock_to_sql.assert_called_once()

def test_load_to_postgresql_invalid(sample_df):
    assert load_to_postgresql(None) is False
    assert load_to_postgresql(sample_df, connection_uri='invalid://localhost/test') is False

@patch('googleapiclient.discovery.build')
@patch('google.oauth2.service_account.Credentials.from_service_account_file')
@patch('os.path.exists')
def test_load_to_google_sheets_success(mock_exists, mock_creds, mock_build, sample_df):
    mock_exists.return_value = True
    mock_service = MagicMock()
    mock_build.return_value = mock_service
    mock_values = MagicMock()
    mock_service.spreadsheets.return_value.values.return_value = mock_values
    mock_values.update.return_value.execute.return_value = {'updatedCells': 14}
    mock_values.clear.return_value.execute.return_value = {}

    res = load_to_google_sheets(
        sample_df,
        spreadsheet_id='test-sheet-id',
        credentials_path='google-sheets-api.json'
    )
    assert res is True

def test_load_to_google_sheets_missing_creds(sample_df):
    res = load_to_google_sheets(
        sample_df,
        spreadsheet_id='test-sheet-id',
        credentials_path='non_existent_creds.json'
    )
    assert res is False

def test_load_to_google_sheets_invalid_df():
    res = load_to_google_sheets(None, spreadsheet_id='test')
    assert res is False
