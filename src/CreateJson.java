import java.io.*;

/**
 * Created by xuzixu on 2017/6/19.
 */
public class CreateJson {
    public static void main(String[] args) {
        String relativePath="/1/2/3/4";
        boolean isPhp=true;
        File file=createJsonFile(relativePath);

        int lastIndexOfSeparator = relativePath.lastIndexOf("/");
        String requestUri = relativePath.substring(0, lastIndexOfSeparator);
        String phpResponse="\t\t\t\"code\" : 1000,\n" +
                "\t\t\t\"bcode\" : 0,\n" +
                "\t\t\t\"message\" : \"\",\n" +
                "\t\t\t\"content\" : null,\n" +
                "\t\t\t\"timeStamp\" : 1234567890\n" ;
        String netOrJavaResponse="\t\t\t\"statusCode\" : 200,\n" +
                "\t\t\t\"success\" : true,\n" +
                "\t\t\t\"message\" : \"\",\n" +
                "\t\t\t\"identity\" : \"\",\n" +
                "\t\t\t\"data\" : null,\n" +
                "\t\t\t\"timeStamp\" : 1234567890\n" ;
        String responseStr;
        if(isPhp){
            responseStr=phpResponse;
        }else{
            responseStr=netOrJavaResponse;
        }
        String jsonStr="[\n" +
                "\t{\n" +
                "\t\t\"request\" : {\n" +
                "\t\t\t\"method\" : \"\",\n" +
                "\t\t\t\"uri\" : \""+requestUri+"\"\n" +
                "\t\t},\n" +
                "\t\t\"response\" : {\n" +
                responseStr +
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
        int lastIndexOfSeparator = relativePath.lastIndexOf("/");
        String filePath = relativePath.substring(1, lastIndexOfSeparator);
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
