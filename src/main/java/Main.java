import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Main {

  public static String bytesToHex(byte[] hash) {
    StringBuilder hex = new StringBuilder(hash.length*2);
    for(int i = 0; i<hash.length; i++) {
      String temp = Integer.toHexString(0xff & hash[i]);
      if(temp.length() == 1) {
        hex.append('0');
      }
      hex.append(temp);
    }
    return hex.toString();
  }

  public static String[] getShaAndFileblob(String fileName) throws IOException, NoSuchAlgorithmException {
    BufferedReader reader = new BufferedReader(new FileReader(fileName));
    StringBuilder content = new StringBuilder();
    String line;
    while((line = reader.readLine()) !=null) {
      content.append(line);
    }
    String fileBlob = "blob "
            +content.length()
            +"\0"
            +content;
    MessageDigest md = MessageDigest.getInstance("SHA-1"); //need to convert this to HEX
    byte[] hash = md.digest(fileBlob.getBytes());
    String hex = bytesToHex(hash);
    System.out.println(hex);
    return new String[]{hex, fileBlob};
  }


  public static String[] getShaAndFileblobFromFile(File file) throws IOException, NoSuchAlgorithmException {
//    BufferedReader reader = new BufferedReader(new FileReader(fileName));
//    StringBuilder content = new StringBuilder();
//    String line;
//    while((line = reader.readLine()) !=null) {
//      content.append(line);
//    }
    byte[] content = Files.readAllBytes(file.toPath());

    String fileBlob = "blob "
            +content.length
            +"\0"
            +content;
    MessageDigest md = MessageDigest.getInstance("SHA-1"); //need to convert this to HEX
    byte[] hash = md.digest(fileBlob.getBytes());
    String hex = bytesToHex(hash);
//    System.out.println(hex);
    return new String[]{hex, fileBlob};
  }



  public static byte[] hexToBinary(String hex) {
    byte[] binary = new byte[20];
    for (int i = 0; i < 20; i++) {
      binary[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
    }
    return binary;
  }

  public static String writeTree(File directory) throws IOException, NoSuchAlgorithmException {
    if (!directory.isDirectory()) return null;

    ByteArrayOutputStream treeContent = new ByteArrayOutputStream();

    // Sort files to match Git's behavior
    File[] files = directory.listFiles();
    if (files != null) {
//      Arrays.sort(files, Comparator.comparing(File::getName));

      for (File file : files) {
        if (file.getName().equals(".git")) continue;  // Ignore .git directory

        String mode;
        String hash;

        if (file.isFile()) {
          mode = "100644";
          hash = getShaAndFileblobFromFile(file)[0];
        } else if (file.isDirectory()) {
          mode = "40000";
          hash = writeTree(file);
        } else {
          continue;
        }

        // Write mode and filename followed by null byte
        treeContent.write((mode + " " + file.getName() + "\0").getBytes());

        // Write binary hash value
        treeContent.write(hexToBinary(hash));
      }
    }

    byte[] treeContentBytes = treeContent.toByteArray();
    String headerString = "tree " + treeContentBytes.length + "\0";
    byte[] headerBytes = headerString.getBytes();

    // Create complete tree object content (header + content)
    ByteArrayOutputStream fullTreeContent = new ByteArrayOutputStream();
    fullTreeContent.write(headerBytes);
    fullTreeContent.write(treeContentBytes);
    byte[] fullBytes = fullTreeContent.toByteArray();

    // Calculate hash of the complete tree object
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    byte[] treeHashBytes = md.digest(fullBytes);
    String hex = bytesToHex(treeHashBytes);

    // Write to file
    File parentDir = new File(".git/objects/" + hex.substring(0, 2));
    if (!parentDir.exists()) parentDir.mkdirs();

    File treeFile = new File(parentDir, hex.substring(2));
    if (!treeFile.exists()) {
      try (OutputStream out = new DeflaterOutputStream(new FileOutputStream(treeFile))) {
        out.write(fullBytes);
      }
    }

    return hex;
  }

  public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

    System.err.println("Logs from your program will appear here!");
//    for(String it: args) {
//      System.out.println("args is: "+it);
//    }
     final String command = args[0];

     switch (command) {
       case "init" -> {
         final File root = new File(".git");
         new File(root, "objects").mkdirs();
         new File(root, "refs").mkdirs();
         final File head = new File(root, "HEAD");

         try {

           head.createNewFile();
           Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
           System.out.println("Initialized git directory");
         } catch (IOException e) {
           throw new RuntimeException(e);
         }
       } case "cat-file" -> {
         String hash = args[2];
         String dirHash = hash.substring(0,2);
         String fileHash = hash.substring(2);
         File blobFile = new File(".git/objects/"+dirHash+"/"+fileHash);

         BufferedReader reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(blobFile))));
//         System.out.println("printed: "+reader.read());
         String blob = reader.readLine();
         String content = blob.substring(blob.indexOf("\0")+1);
         System.out.print(content);
//         StringBuilder content = new StringBuilder();
//         String line;
//
//         while ((line = reader.readLine()) != null) {
//           content.append(line);
//           content.append(System.lineSeparator());
//         }
//         System.out.println(content);
       } case "hash-object" -> {
         String fileName = args[2];
         String[] result = getShaAndFileblob(fileName);
         String hex = result[0];
         String fileBlob = result[1];
         File parentDir = new File(".git/objects/" + hex.substring(0,2));
         if (!parentDir.exists()) {
           parentDir.mkdirs();
         }

         File file = new File(parentDir + "/"+ hex.substring(2));
         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new DeflaterOutputStream(new FileOutputStream(file))));
         writer.write(fileBlob);
         writer.close();
         //use hash from args to open the tree object, and extract directory names from it
       } case "ls-tree" -> {
         String treeHash = args[2];
         File file = new File(".git/objects/" + treeHash.substring(0,2) + "/" + treeHash.substring(2));
         BufferedReader reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(file))));
         String line;
         StringBuilder treeObjectContent = new StringBuilder();
         while((line = reader.readLine()) != null) {
           treeObjectContent.append(line);
         }
         String temp = new String(treeObjectContent);
         String[] array = temp.split("\0");

//         System.out.println("my array is: "+Arrays.toString(array));

         ArrayList<String> dirStructure = new ArrayList<>();
         for(int i = 1; i<array.length; i++) {
           String[] tempArray = array[i].split(" ",2);
           if(tempArray.length == 2) {
             String name = tempArray[1];
             dirStructure.add(name);
           }
         }
         dirStructure.sort(null);
//         System.out.println("------dirStructure: "+ dirStructure);
         for(String s : dirStructure) {
           System.out.println(s);
         }
       } case "write-tree" -> {
         File file = new File(".");
         String treeHash = writeTree(file);
         System.out.println(treeHash);
       }
       default -> System.out.println("Unknown command: " + command);
     }
  }
}
