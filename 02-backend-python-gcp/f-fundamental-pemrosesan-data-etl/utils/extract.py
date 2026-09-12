import requests
from bs4 import BeautifulSoup
from datetime import datetime
import time

BASE_URL = "https://fashion-studio.dicoding.dev"

def scrape_page(page_number, session=None):
    """
    Mengambil data produk dari 1 halaman website Fashion Studio.
    """
    try:
        if session is None:
            session = requests.Session()

        path = "" if page_number == 1 else f"page{page_number}"
        url = f"{BASE_URL}/{path}"

        response = session.get(url, timeout=15)
        response.raise_for_status()

        timestamp = datetime.now().isoformat()
        soup = BeautifulSoup(response.text, 'html.parser')
        cards = soup.find_all('div', class_='collection-card')

        page_data = []
        for card in cards:
            # Title
            title_tag = card.find('h3', class_='product-title')
            title = title_tag.get_text(strip=True) if title_tag else None

            # Price
            price_tag = card.find('span', class_='price') or card.find('p', class_='price')
            price = price_tag.get_text(strip=True) if price_tag else None

            # Details: Rating, Colors, Size, Gender
            rating, colors, size, gender = None, None, None, None
            details_container = card.find('div', class_='product-details')
            if details_container:
                for p in details_container.find_all('p'):
                    text = p.get_text(strip=True)
                    if 'Rating:' in text:
                        rating = text
                    elif 'Color' in text:
                        colors = text
                    elif 'Size:' in text:
                        size = text
                    elif 'Gender:' in text:
                        gender = text

            page_data.append({
                'Title': title,
                'Price': price,
                'Rating': rating,
                'Colors': colors,
                'Size': size,
                'Gender': gender,
                'timestamp': timestamp
            })
        return page_data
    except requests.exceptions.RequestException as e:
        print(f"Error fetching page {page_number}: {e}")
        return None
    except Exception as e:
        print(f"An unexpected error occurred while parsing page {page_number}: {e}")
        return None

def scrape_main(total_pages=50):
    """
    Mengambil seluruh data dari halaman 1 sampai total_pages (default: 50).
    """
    products = []
    try:
        session = requests.Session()
        for page in range(1, total_pages + 1):
            page_data = scrape_page(page, session=session)
            if page_data:
                products.extend(page_data)
        return products
    except requests.exceptions.RequestException as e:
        print(f"Error fetching website: {e}")
        return None
    except Exception as e:
        print(f"An error occurred during scraping: {e}")
        return None
