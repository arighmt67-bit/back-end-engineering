import pytest
from unittest.mock import patch, MagicMock
import requests
from utils.extract import scrape_page, scrape_main

SAMPLE_HTML = """
<div class="collection-card">
  <div style="position: relative;">
    <img alt="T-shirt 2" class="collection-image" src="https://picsum.photos/280/350?random=2"/>
  </div>
  <div class="product-details">
    <h3 class="product-title">T-shirt 2</h3>
    <div class="price-container"><span class="price">$102.15</span></div>
    <p style="font-size: 14px; color: #777;">Rating: ⭐ 3.9 / 5</p>
    <p style="font-size: 14px; color: #777;">3 Colors</p>
    <p style="font-size: 14px; color: #777;">Size: M</p>
    <p style="font-size: 14px; color: #777;">Gender: Women</p>
  </div>
</div>
<div class="collection-card">
  <div class="product-details">
    <h3 class="product-title">Unknown Product</h3>
    <div class="price-container"><span class="price">$100.00</span></div>
    <p>Rating: ⭐ Invalid Rating / 5</p>
    <p>5 Colors</p>
    <p>Size: M</p>
    <p>Gender: Men</p>
  </div>
</div>
"""

def test_scrape_page_success():
    mock_session = MagicMock()
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.text = SAMPLE_HTML
    mock_session.get.return_value = mock_response

    data = scrape_page(1, session=mock_session)
    assert data is not None
    assert len(data) == 2
    assert data[0]['Title'] == 'T-shirt 2'
    assert data[0]['Price'] == '$102.15'
    assert data[0]['Rating'] == 'Rating: ⭐ 3.9 / 5'
    assert data[0]['Colors'] == '3 Colors'
    assert data[0]['Size'] == 'Size: M'
    assert data[0]['Gender'] == 'Gender: Women'
    assert 'timestamp' in data[0]

def test_scrape_page_http_error():
    mock_session = MagicMock()
    mock_session.get.side_effect = requests.exceptions.RequestException("Connection timed out")

    data = scrape_page(1, session=mock_session)
    assert data is None

def test_scrape_page_unexpected_error():
    mock_session = MagicMock()
    mock_session.get.side_effect = Exception("General failure")

    data = scrape_page(1, session=mock_session)
    assert data is None

def test_scrape_page_soup_exception():
    mock_session = MagicMock()
    mock_response = MagicMock()
    mock_response.status_code = 200
    # Text that might break bs4 parsing logic
    mock_response.text = None
    mock_session.get.return_value = mock_response

    data = scrape_page(1, session=mock_session)
    assert data is None

@patch('utils.extract.scrape_page')
def test_scrape_main_success(mock_scrape_page):
    mock_scrape_page.return_value = [{'Title': 'T-shirt 2', 'Price': '$100.00'}]
    result = scrape_main(total_pages=3)
    assert result is not None
    assert len(result) == 3
    assert mock_scrape_page.call_count == 3

@patch('utils.extract.scrape_page')
def test_scrape_main_exception(mock_scrape_page):
    mock_scrape_page.side_effect = requests.exceptions.RequestException("Network error")
    result = scrape_main(total_pages=2)
    assert result is None

@patch('utils.extract.requests.Session')
def test_scrape_main_general_exception(mock_session):
    mock_session.side_effect = Exception("Crash")
    result = scrape_main(total_pages=2)
    assert result is None
