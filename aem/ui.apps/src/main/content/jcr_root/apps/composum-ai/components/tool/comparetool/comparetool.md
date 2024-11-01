# Compare Tool Specification

## Overview

This document outlines the specifications for a single-page HTML application named `comparetool.html`. The application
will display two web pages side by side, allowing for an efficient comparison.

## Features

1. **Two Iframes Side by Side**: The application will consist of two iframes, each occupying 50% of the browser width
   and the full height of the window.

2. **URL Fields**: Each iframe will have a dedicated URL field above it for users to input the URLs they want displayed
   within the iframes.

3. **Synchronized Scrolling**: If one iframe is scrolled, the other iframe will scroll in proportion. For instance, if
   the left iframe is scrolled to 42%, the right iframe will also scroll to 42%.

4. **Parameters**: the URL parameters passed to the application are:
   - `url1`: the URL that is initially loaded into the left iframe
   - `url2`: the URL that is initially loaded into the right iframe

The tool is meant to be run in a desktop browser, no responsive design necessary.

## Implementation Details

- **HTML Structure**: The HTML will consist of two main sections, each containing an iframe and a URL input field.
- **CSS Styling**: The iframes will be styled to take up 50% of the width and 100% height of the viewport.
- **JavaScript Functionality**: A script will be implemented to handle the synchronization of scrolling between the two
  iframes.

## Conclusion

The `comparetool.html` application is designed to provide users with an efficient means of comparing two web pages side
by side. With synchronized scrolling and dedicated URL fields, users can easily navigate and assess differences between
the two sources.

## TODOs / ideas

- if url1/url2 are changed, the location needs to be updated (push state!)
- url1/url2 should set the fields
- default url1 to blueprint of url2 ; button to do that
- styling
- checkbox to break scroll synchronization
- shortcut to editor
- bookmarklet to open comparison
- help texts

