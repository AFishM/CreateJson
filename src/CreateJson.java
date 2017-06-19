import java.io.*;

/**
 * Created by xuzixu on 2017/6/19.
 */
public class CreateJson {
    public static void main(String[] args) {
        String relativePath="/1/2/3/4";
        File file=createJsonFile(relativePath);

        int lastIndexOfSeparator = relativePath.lastIndexOf("/");
        String filePath = relativePath.substring(1, lastIndexOfSeparator);
        String jsonStr="[\n" +
                "\t{\n" +
                "\t\t\"request\" : {\n" +
                "\t\t\t\"method\" : \"\",\n" +
                "\t\t\t\"uri\" : \""+filePath+"\"\n" +
                "\t\t},\n" +
                "\t\t\"response\" : {\n" +
                "\n" +
                "\t\t}\n" +
                "\t}\n" +
                "]";

        try {
            FileWriter fileWriter=new FileWriter(file);
            fileWriter.write(jsonStr);
            fileWriter.close();
        } catch (Exception e) {
            System.out.print("e=> "+e.toString());
            e.printStackTrace();
        }
    }

    private static File createJsonFile(String relativePath) {
        relativePath=relativePath.substring(1);
        int lastIndexOfSeparator = relativePath.lastIndexOf("/");
        String filePath = relativePath.substring(0, lastIndexOfSeparator);
        File dir = new File(filePath);
        if (!dir.exists()) {
            boolean makeDirsSuccess = dir.mkdirs();
            if (!makeDirsSuccess) {
                throw new RuntimeException("make dirs " + filePath + " fail");
            }
        }

        String jsonFileName = relativePath.substring(lastIndexOfSeparator + 1) + ".json";
        File jsonFile = new File(filePath + "/" + jsonFileName);
        try {
            boolean createJsonFileSuccess = jsonFile.createNewFile();
            if (!createJsonFileSuccess) {
                throw new RuntimeException("create json file " + jsonFileName + " fail");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonFile;
    }
}
