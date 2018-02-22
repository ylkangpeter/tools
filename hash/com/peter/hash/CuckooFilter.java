package com.peter.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * detail about cuckoofilter: http://www.cs.cmu.edu/~binfan/papers/conext14_cuckoofilter.pdf
 *
 * @Author ylkang
 * @Date Feb 15th, 2016
 */
public class CuckooFilter {

    private final static String DEFAULT_CHARSET = "utf-8";

    private final static int MAX_DEPTH = 256;
    private final static int SIZE = 1024;
    private final static int SLOT_SIZE = 4;

    private final CuckooEntry[][] entries = new CuckooEntry[SIZE][SLOT_SIZE];

    /**
     * two hash function to calc the slots
     *
     * @param sha1
     * @param size
     * @return
     */
    private static int hash1(byte[] sha1, int size) {
        return ((sha1[0] << 3) | (sha1[1] << 2) | (sha1[2] << 1) | sha1[3]) & (size - 1);
    }

    private static int hash2(byte[] sha1, int size) {
        return ((sha1[4] << 3) | (sha1[5] << 2) | (sha1[6] << 1) | sha1[7]) & (size - 1);
    }


    /**
     * sha1 function
     *
     * @param source
     * @return
     * @throws NoSuchAlgorithmException
     */
    private static byte[] sha1(String source) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
        messageDigest.update(source.getBytes());
        return messageDigest.digest();
    }

    public boolean get(String key) {
        try {
            byte[] encodeKey = sha1(key);
            int inx1 = hash1(encodeKey, SIZE);
            int inx2 = hash2(encodeKey, SIZE);

            return validateKey(entries[inx1], encodeKey) || validateKey(entries[inx2], encodeKey);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace(); //TODO handle later
        }
        return false;
    }

    private boolean validateKey(CuckooEntry[] entry, byte[] encodeKey) {
        for (int i = 0; i < entry.length; i++) {
            if (entry == null || entry[i] == null || entry[i].getKey() == null || entry[i].getKey().length != encodeKey.length || entry[i].getState().equals(EntryState.DELETED)) {
                continue;
            }
            for (int j = 0; j < entry[i].getKey().length; j++) {
                if (CuckooFilterUtil.isSame(entry[i].getKey(), encodeKey)) {
                    return true;
                }
            }
        }
        return false;
    }


    public void put(String key) {
        try {
            byte[] encodeKey = sha1(key);
            int inx1 = hash1(encodeKey, SIZE);
            int inx2 = hash2(encodeKey, SIZE);

            if (!trySlot(inx1, inx2, encodeKey) || !trySlot(inx2, inx1, encodeKey)) {
                if (!putElement(encodeKey, 0, inx1, inx2)) {
                    System.out.println("asdfasdfasdf");
                    // TODO rehash
                }
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace(); //TODO handle later
        }

    }

    private boolean trySlot(int inx1, int inx2, byte[] encodeKey) {
        for (int i = 0; i < entries[inx1].length; i++) {
            if (entries[inx1][i] == null) {
                entries[inx1][i] = new CuckooEntry(encodeKey, inx2);
                return true;
            } else if (entries[inx1][i].getState().equals(EntryState.DELETED)) {
                entries[inx1][i].setState(EntryState.FILLED);
                entries[inx1][i].anotherChoice = inx2;
                entries[inx1][i].setKey(encodeKey);
            }
        }
        return false;
    }

    private boolean putElement(byte[] encodeKey, int counter, int inx1, int inx2) {
        while (counter < MAX_DEPTH) {
            counter++;
            if (!trySlot(inx1, inx2, encodeKey)) {
                // rotate
                byte[] tmpKey = encodeKey;
                int tmpInx = inx1;
                // fill to-do value
                encodeKey = entries[inx1][0].getKey();
                inx1 = entries[inx1][0].getAnotherChoice();
                // set data
                entries[tmpInx][0].setKey(tmpKey);
                entries[tmpInx][0].setAnotherChoice(inx2);

                if (entries[inx1] == null) {
                    entries[inx1] = new CuckooEntry[SLOT_SIZE];
                    entries[inx1][0] = new CuckooEntry(encodeKey, tmpInx);
                }
            } else {
                return true;
            }
        }

        counter = 0;
        while (counter < MAX_DEPTH) {
            counter++;
            if (!trySlot(inx1, inx2, encodeKey)) {
                // rotate
                byte[] tmp = encodeKey;
                encodeKey = entries[inx1][0].getKey();
                entries[inx1][0].setKey(tmp);
                int tmpInx = inx1;
                inx1 = entries[inx1][0].getAnotherChoice();
                if (entries[inx1] == null) {
                    entries[inx1] = new CuckooEntry[SLOT_SIZE];
                    entries[inx1][0] = new CuckooEntry(encodeKey, tmpInx);
                }
                entries[inx1][0].setAnotherChoice(tmpInx);
            } else {
                return true;
            }
        }
        return false;
    }

    private enum EntryState {
        NEW, FILLED, DELETED;
    }

    private static class RotateResult {
        boolean exists = false;
        boolean done = true;
        int inx;
        byte[] key;

        public RotateResult(boolean status) {
            this.done = status;
        }
    }

    private static class CuckooEntry {

        private EntryState state = EntryState.NEW;
        private int anotherChoice;
        private byte[] key;

        public CuckooEntry(byte[] key, int anotherChoice) {
            this.state = EntryState.FILLED;
            this.key = key;
            this.anotherChoice = anotherChoice;
        }

        public int getAnotherChoice() {
            return anotherChoice;
        }

        public void setAnotherChoice(int anotherChoice) {
            this.anotherChoice = anotherChoice;
        }

        public EntryState getState() {
            return state;
        }

        public void setState(EntryState state) {
            this.state = state;
        }

        public byte[] getKey() {
            return key;
        }

        public void setKey(byte[] key) {
            this.key = key;
        }

    }

    public static void main(String[] args) {
        CuckooFilter filter = new CuckooFilter();
        for (int i = 0; i < 800; i++) {
            filter.put(String.valueOf(i));
        }
        boolean result = true;
        for (int i = 0; i < 800; i++) {
            System.out.println(i);
            result &= filter.get(String.valueOf(i));
        }
        System.out.println(result);

        for (int i = 801; i < 10000; i++) {
            if (filter.get(String.valueOf(i))) {
                System.out.println(i + ": fail");
            }
        }
    }

}