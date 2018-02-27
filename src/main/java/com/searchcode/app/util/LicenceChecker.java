package com.searchcode.app.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.searchcode.app.config.Values;
import com.searchcode.app.model.LicenseMatch;
import com.searchcode.app.model.LicenseResult;
import com.searchcode.app.service.Singleton;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LicenceChecker {
    private final Vectorspace vectorSpace;
    private String DATABASEPATH = Properties.getProperties().getProperty(Values.LICENSE_DATABASE_LOCATION, Values.DEFAULT_LICENSE_DATABASE_LOCATION);
    private ArrayList<LicenseResult> database = new ArrayList<>();

    public LicenceChecker() {
        this.database = this.loadDatabase();
        this.vectorSpace = new Vectorspace();
    }

    public List<LicenseResult> getDatabase() {
        return this.database;
    }

    public void processFile() {
//        licenseGuesses = guessLicense(string(content), deepGuess, loadDatabase())
//        licenseIdentified = identifierGuessLicence(string(content), loadDatabase())
    }

    /**
     * Given a string will scan through it using keywords to try and
     * identify which license it has
     */
    public List<LicenseMatch> keywordGuessLicense(String content) {
        List<LicenseMatch> licenseMatches = Collections.synchronizedList(new ArrayList<>());
        String cleanContent = this.vectorSpace.cleanText(content);

        // Parallel stream is about 3x faster for this
        this.database.parallelStream().forEach(licenseResult -> {
            int keywordMatch = 0;

            for (String keyword: licenseResult.keywords) {
                if (content.contains(keyword)) {
                    keywordMatch++;
                }
            }

            if (keywordMatch >= 1) {
                float percentage = (float)keywordMatch / (float)licenseResult.keywords.size();
                licenseMatches.add(new LicenseMatch(licenseResult.licenseId, percentage));
            }
        });

        return licenseMatches;
    }


    public ArrayList<LicenseResult> guessLicense(String content) {
        for (LicenseResult licenseResult: this.database) {

        }
        return null;
    }

//    func guessLicense(content string, deepguess bool, licenses []License) []LicenseMatch {
//        matchingLicenses := []LicenseMatch{}
//
//        for _, license := range keywordGuessLicense(content, licenses) {
//            matchingLicense := License{}
//
//            for _, l := range licenses {
//                if l.LicenseId == license.LicenseId {
//                    matchingLicense = l
//                    break
//                }
//            }
//
//            runecontent := []rune(content)
//            trimto := utf8.RuneCountInString(matchingLicense.LicenseText)
//
//            if trimto > len(runecontent) {
//                trimto = len(runecontent)
//            }
//
//            contentConcordance := vectorspace.BuildConcordance(string(runecontent[:trimto]))
//            relation := vectorspace.Relation(matchingLicense.Concordance, contentConcordance)
//
//            if relation >= confidence {
//                matchingLicenses = append(matchingLicenses, LicenseMatch{LicenseId: license.LicenseId, Percentage: relation})
//            }
//        }
//
//        if len(matchingLicenses) == 0 && deepguess == true {
//            for _, license := range licenses {
//                runecontent := []rune(content)
//                trimto := utf8.RuneCountInString(license.LicenseText)
//
//                if trimto > len(runecontent) {
//                    trimto = len(runecontent)
//                }
//
//                contentConcordance := vectorspace.BuildConcordance(string(runecontent[:trimto]))
//                relation := vectorspace.Relation(license.Concordance, contentConcordance)
//
//                if relation >= confidence {
//                    matchingLicenses = append(matchingLicenses, LicenseMatch{LicenseId: license.LicenseId, Percentage: relation})
//                }
//            }
//        }
//
//        sort.Slice(matchingLicenses, func(i, j int) bool {
//            return matchingLicenses[i].Percentage > matchingLicenses[j].Percentage
//        })
//
//        return matchingLicenses
//    }


    /**
     * Looks for licenses using the SPDX License Identifier syntax
     */
    public List<String> identifierGuessLicence(String content) {
        Matcher matcher = Pattern.compile("SPDX-License-Identifier:\\s+(.*?)[ |\\n|\\r\\n]").matcher(content);

        ArrayList<String> matches = new ArrayList<>();

        while (matcher.find()) {
            matches.add(matcher.group(1));
        }

        return matches;
    }

    /**
     * Loads the License database from a JSON file on disk
     */
    private ArrayList<LicenseResult> loadDatabase() {
        ArrayList<LicenseResult> database = new ArrayList<>();

        try {
            Gson gson = new GsonBuilder().create();
            LicenseResult[] myArray = gson.fromJson(new FileReader(this.DATABASEPATH), LicenseResult[].class);
            database = new ArrayList<>(Arrays.asList(myArray));

            Vectorspace vec = new Vectorspace();

            for (LicenseResult licenseResult: database) {
                licenseResult.concordance = vec.concordance(licenseResult.licenseText);
            }
        }
        catch (FileNotFoundException | JsonSyntaxException ex) {
            Singleton.getLogger().warning("Unable to load License Database from disk " + ex);
        }

        return database;
    }
}
