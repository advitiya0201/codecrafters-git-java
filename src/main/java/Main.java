import java.io.*;
import java.nio.Buffer;
import java.nio.file.Files;
import java.util.zip.InflaterInputStream;

public class Main {
  public static void main(String[] args) throws IOException {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");
    for(String it: args) {
      System.out.println("args is: "+it);
    }
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
//
//         System.out.println(content);
       } case "hash-object" -> {
         String fileName = args[2];
         BufferedReader reader = new BufferedReader(new FileReader(fileName));
         StringBuilder content = new StringBuilder();
         String line;
         while((line = reader.readLine()) !=null) {
           content.append(line);
           content.append(System.lineSeparator());
         }
         System.out.println("file content is: "+content);
       }
       default -> System.out.println("Unknown command: " + command);
     }
  }
}
