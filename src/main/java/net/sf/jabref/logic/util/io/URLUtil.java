/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.util.io;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.logic.util.DOI;

public class URLUtil {

    private static final String URL_EXP = "^(https?|ftp)://.+";


    /**
     * Cleans URLs returned by Google search.
     *
     * <example>
     *  If you copy links from search results from Google, all links will be enriched with search meta data, e.g.
     *  https://www.google.de/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&&url=http%3A%2F%2Fwww.inrg.csie.ntu.edu.tw%2Falgorithm2014%2Fhomework%2FWagner-74.pdf&ei=DifeVYHkDYWqU5W0j6gD&usg=AFQjCNFl638rl5KVta1jIMWLyb4CPSZidg&sig2=0hSSMw9XZXL3HJWwEcJtOg
     * </example>
     *
     * @param url the Google search URL string
     * @return the cleaned Google URL or @code{url} if no search URL was detected
     */
    public static String cleanGoogleSearchURL(String url) {
        Objects.requireNonNull(url);

        // Detect Google search URL
        final String searchExp = "^https?://(?:www\\.)?google\\.[\\.a-z]+?/url.*";

        if(!url.matches(searchExp)) {
            return url;
        }
        // Extract destination URL
        try {
            URL searchURL = new URL(url);
            // URL parameters
            String query = searchURL.getQuery();
            // no parameters
            if (query == null) {
                return url;
            }
            // extract url parameter
            String[] pairs = query.split("&");

            for (String pair: pairs) {
                // "clean" url is decoded value of "url" parameter
                if (pair.startsWith("url=")) {
                    String value = pair.substring(pair.indexOf("=") + 1, pair.length());

                    String decode = URLDecoder.decode(value, "UTF-8");
                    // url?
                    if(decode.matches(URL_EXP)) {
                        return decode;
                    }
                }
            }
            return url;
        } catch (UnsupportedEncodingException | MalformedURLException e) {
            return url;
        }
    }


    /**
     * Make sure an URL is "portable", in that it doesn't contain bad characters that break the open command in some
     * OSes.
     *
     * A call to this method will also remove \\url{} enclosings and clean Doi links.
     *
     * @param link :the URL to sanitize.
     * @return Sanitized URL
     */
    public static String sanitizeUrl(String link) {
        link = link.trim();
    
        // First check if it is enclosed in \\url{}. If so, remove the wrapper.
        if (link.startsWith("\\url{") && link.endsWith("}")) {
            link = link.substring(5, link.length() - 1);
        }
    
        // DOI cleanup
        // converts doi-only link to full http address
        // Morten Alver 6 Nov 2012: this extracts a nonfunctional Doi from some complete
        // http addresses (e.g. http://onlinelibrary.wiley.com/doi/10.1002/rra.999/abstract, where
        // the trailing "/abstract" is included but doesn't lead to a resolvable Doi).
        // To prevent mangling of working URLs I'm disabling this check if the link is already
        // a full http link:
        // TODO: not sure if this is allowed
        if (link.matches("^doi:/*.*")) {
            // Remove 'doi:'
            link = link.replaceFirst("^doi:/*", "");
            link = new DOI(link).getURLAsASCIIString();
        }
    
        Optional<DOI> doi = DOI.build(link);
        if (doi.isPresent() && !link.matches("^https?://.*")) {
            link = doi.get().getURLAsASCIIString();
        }
    
        // FIXME: everything below is really flawed atm
        link = link.replaceAll("\\+", "%2B");
    
        try {
            link = URLDecoder.decode(link, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            // Ignored
        }
    
        /**
         * Fix for: [ 1574773 ] sanitizeUrl() breaks ftp:// and file:///
         *
         * http://sourceforge.net/tracker/index.php?func=detail&aid=1574773&group_id=92314&atid=600306
         */
        try {
            return new URI(null, link, null).toASCIIString();
        } catch (URISyntaxException e) {
            return link;
        }
    }
}