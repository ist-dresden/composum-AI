#!/bin/bash

SITEMAP="target/site/sitemap.xml"
mkdir -p target/site

# Start the sitemap file
echo '<?xml version="1.0" encoding="UTF-8"?>' > $SITEMAP
echo '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">' >> $SITEMAP

# For each .md file in the repository
for file in $(find src/site/markdown -type f -name "*.md"); do
    # Get the last commit date for the file
    last_commit_date=$(git log -1 --format=%cd --date=format:'%Y-%m-%d' -- "$file")
    
    # Convert the .md file path to .html
    html_path="${file%.md}.html"

    echo $html_path

    # Remove the leading src/site/markdown/
    html_path="${html_path#src/site/markdown/}"

    if [ "$html_path" == "index.html" ]; then
        html_path=""
    fi

    # Append the entry to the $SITEMAP
echo "  <url>" >> $SITEMAP
echo "    <loc>https://ai.composum.com/$html_path</loc>" >> $SITEMAP
echo "    <lastmod>$last_commit_date</lastmod>" >> $SITEMAP
echo "  </url>" >> $SITEMAP
done

# Close the $SITEMAP file
echo '</urlset>' >> $SITEMAP
