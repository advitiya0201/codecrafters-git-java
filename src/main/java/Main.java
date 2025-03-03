import java.io.*;
import java.nio.Buffer;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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
//           System.out.println("temp is: "+temp);
           treeObjectContent.append(line);
         }
         String temp = new String(treeObjectContent);
         String[] array = temp.split("\0");
         System.out.println("my array is: "+Arrays.toString(array));

       }
       default -> System.out.println("Unknown command: " + command);
     }
  }
}
