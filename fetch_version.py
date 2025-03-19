#!/usr/bin/env python3
"""Script to fetch the latest app version and build number and save to version.json file."""

import argparse
import json
import os
from typing import Any, Dict, List, Optional, Tuple, Union

import requests
from bs4 import BeautifulSoup


def get_latest_version() -> Tuple[str, str]:
    """
    Get the latest version & build of the app.

    Returns:
        Tuple[str, str]: A tuple containing (app_version, build_number)

    Raises:
        Exception: If there's an error fetching or parsing the version information
    """
    url: str = (
        'https://www.apkmirror.com/apk/grindr-llc/grindr-gay-chat-meet-date/'
    )
    base_url: str = url.split('/apk/')[0]

    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36'
    }

    response: requests.Response = requests.get(url, headers=headers, timeout=30)
    response.raise_for_status()

    soup: BeautifulSoup = BeautifulSoup(response.text, 'html.parser')
    latest_links = soup.select('.appRowTitle > a')

    if not latest_links:
        raise ValueError('Could not find latest version link')

    latest_href: str = latest_links[0]['href']
    version_url: str = f'{base_url}{latest_href}'

    response = requests.get(version_url, headers=headers, timeout=30)
    response.raise_for_status()

    soup = BeautifulSoup(response.text, 'html.parser')

    variant_tables = soup.select('.variants-table')
    if not variant_tables:
        raise ValueError('Could not find variants table')

    table_rows = variant_tables[0].select('.table-row')
    if not table_rows:
        raise ValueError('Could not find table rows')

    cells = table_rows[-1].select('.table-cell')
    if not cells:
        raise ValueError('Could not find table cells')

    app_data: List[str] = cells[0].text.strip().split('\n')
    app_data = [line.strip() for line in app_data]

    if not app_data:
        raise ValueError('Could not parse app data')

    app_version: str = app_data[0]

    digits = [int(s) for s in app_data if s.isdigit()]
    if not digits:
        raise ValueError('Could not find build number')

    build_number: int = digits[0]

    return app_version, str(build_number)


def save_version_to_json(version: str, build: str, output_file: str) -> None:
    """
    Save version information to a JSON file.

    Args:
        version: The version name (e.g., "25.3.0")
        build: The build number (e.g., "135731")
        output_file: Path to the output JSON file
    """
    data: Dict[str, Union[str, int]] = {
        'versionName': version,
        'versionCode': int(build),
    }

    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)

    print(f'Version information saved to {output_file}')


def parse_args() -> argparse.Namespace:
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(
        description='Fetch latest app version and build number'
    )
    parser.add_argument(
        '-o',
        '--output',
        default='version.json',
        help='Output JSON file path (default: version.json)',
    )
    return parser.parse_args()


def main() -> None:
    """Main function to fetch version information and save to file."""
    args = parse_args()
    version, build = get_latest_version()
    print(f'App Version: {version}')
    print(f'Build Number: {build}')
    save_version_to_json(version, build, args.output)


if __name__ == '__main__':
    main()
