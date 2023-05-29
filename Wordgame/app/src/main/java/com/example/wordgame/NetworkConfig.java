package com.example.wordgame;

import java.util.HashMap;

/* !!! IMPORTANT !!!
For anyone building this project from source, you MUST modify this file to match your environment

Replace both 'versionURL' and 'boardListURL' with your own links
The contents of the files behind these URLs are as follows:
- versionURL:
    - Plain text version number of the applications current version
    - Example: 1.0.0
- boardListURL:
    - A list of plain text URLs that lead to board files
    - Example:
        url to boardFile1
        url to boardFile2
        url to boardFile3

- boardFile:
    - boardFiles are XML format text files with the following schema:
        <count>1</count>
        <board>
            <tiles>abcdefghijklmnop</tiles>
            <score>999</score>
            <words>
                <word>example<word>
            </words>
        </board>

        ,where
            - count: Integer, the number of boards in this file (the number of board-elements)
            - board: A board element, that contains
                - tiles: String, the 4x4 board letters, starting from top-left to bottom-right
                         the example above provides a board of type
                         a b c d
                         e f g h
                         i j k l
                         m n o p
                - words: A list of all the words that can be found on this board, contains word elements
                    - word: String, a word that can be found on this board
    - An example boardFile can be found here:
        - https://drive.google.com/file/d/1I8nfebuCtRsj0iCMPMlPcpdL22e3Xp7A/view?usp=sharing
 */

public class NetworkConfig {

    // REPLACE THIS URL WITH YOUR OWN
    private static final String VERSION_URL = "DUMMY-URL-REPLACE-ME";

    // REPLACE THIS URL WITH YOUR OWN
    private static final String BOARD_LIST_URL = "DUMMY-URL-REPLACE-ME";

    // REPLACE THIS URL WITH YOUR OWN
    private static final String MESSAGE_URL = "DUMMY-URL-REPLACE-ME";

    private static final HashMap<String, String> URLCONFIG = new HashMap<String, String>() {
        {
            put("version", VERSION_URL);
            put("boardList", BOARD_LIST_URL);
            put("message", MESSAGE_URL);
        }
    };

    public static String getUrl(String type) throws InvalidUrlRequestException {
        if(!URLCONFIG.containsKey(type)) {
            throw new InvalidUrlRequestException("The requested URL type was not found");
        }
        return URLCONFIG.get(type);
    }
}

