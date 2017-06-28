import com.google.gson.Gson;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * make json data for moco
 * Created by xuzixu on 2017/6/19.
 */
public class CreateJson {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("please enter necessary args");
            return;
        }
        String entityClassFileName = args[0];
        if (!entityClassFileName.contains(File.separator)) {
            String currentDir=System.getProperty("user.dir");
            entityClassFileName = currentDir+File.separator + entityClassFileName;
        }
        String relativePath = args[1];
        boolean isPhp = args[2].toLowerCase().equals("true");

        File entityFile = new File(entityClassFileName);
        removeEntityClassPackageName(entityFile);
        compile(entityFile);
        String className = entityFile.getName().split("\\.")[0];
        String responseJsonStr = entityConvertToJsonStr(className, entityFile.getParentFile().getAbsolutePath());
        deleteFile(className, entityFile.getParent());

        File jsonFile = createJsonFile(relativePath);
        writeJsonToFile(responseJsonStr, jsonFile, relativePath, isPhp);
        includeToServer(relativePath);
    }

    private static void removeEntityClassPackageName(File entityClassFileName) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(entityClassFileName));
            String fileContent = "";
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("package")) {
                    continue;
                }
                fileContent = fileContent + line + "\n";
            }
            bufferedReader.close();
            FileWriter fileWriter = new FileWriter(entityClassFileName);
            fileWriter.write(fileContent);
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void compile(File entityClassFileName) {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager javaFileManager = javaCompiler.getStandardFileManager(null, null, null);
        Iterable compilationUnits = javaFileManager.getJavaFileObjects(entityClassFileName);
        JavaCompiler.CompilationTask compilationTask = javaCompiler.getTask(null, javaFileManager, null, null, null, compilationUnits);
        compilationTask.call();
        try {
            javaFileManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String entityConvertToJsonStr(String className, String targetClassPath) {
        try {
            URL classUrl = new URL("file:/" + targetClassPath + "/");
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{classUrl}, CreateJson.class.getClassLoader());
            Class entityClass = urlClassLoader.loadClass(className);
            Object object = classToObject(entityClass, null, null);
            Gson gson = new Gson();
            String json = gson.toJson(object);
            String formatJsonStr = formatJson(json);
            formatJsonStr = formatJsonStr.replaceAll("\"", "\\\"");
            return formatJsonStr;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Object classToObject(Class c, Object parentObject, Class parentClass) {
        Object object = null;
        try {
            if (parentObject == null) {
                object = c.newInstance();
            } else {
                Constructor[] constructors = c.getDeclaredConstructors();
                for (Constructor constructor : constructors) {
                    Class[] classes = constructor.getParameterTypes();
                    if (classes.length == 0) {
                        object = constructor.newInstance();
                        break;
                    }
                    if (classes.length == 1 && classes[0] == parentClass) {
                        object = constructor.newInstance(parentObject);
                        break;
                    }
                }
                if (object == null) {
                    return null;
                }
            }

            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                fieldSetter(field, object, c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    private static void fieldSetter(Field field, Object declaringClassInstance, Class declaringClass) {
        if (field.getDeclaringClass() != declaringClass) {
            return;
        }
        Class fieldClass = field.getType();
        if (fieldClass.isPrimitive()) {
            return;
        }

        if (Modifier.toString(field.getModifiers()).equals("final")) {
            return;
        }
        if (Modifier.toString(field.getModifiers()).equals("static")) {
            return;
        }

        field.setAccessible(true);
        if (fieldClass == List.class) {
            Type type = field.getGenericType();
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Class realFieldClass = (Class) parameterizedType.getActualTypeArguments()[0];

                List objectList = new ArrayList();
                for (int i = 0; i < 10; i++) {
                    objectList.add(classToObject(realFieldClass, declaringClassInstance, declaringClass));
                }
                try {
                    field.set(declaringClassInstance, objectList);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return;
        }
        if (fieldClass == String.class) {
            try {
                field.set(declaringClassInstance, field.getName());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            field.set(declaringClassInstance, classToObject(fieldClass, declaringClassInstance, declaringClass));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static String formatJson(String jsonStr) {
        if (null == jsonStr || "".equals(jsonStr)) return "";
        StringBuilder sb = new StringBuilder();
        char current;
        int indent = 0;
        for (int i = 0; i < jsonStr.length(); i++) {
            current = jsonStr.charAt(i);
            switch (current) {
                case '{':
                case '[':
                    sb.append(current);
                    sb.append('\n');
                    indent++;
                    addIndentBlank(sb, indent);
                    break;
                case '}':
                case ']':
                    sb.append('\n');
                    indent--;
                    addIndentBlank(sb, indent);
                    sb.append(current);
                    break;
                case ',':
                    sb.append(current);
                    sb.append('\n');
                    addIndentBlank(sb, indent);
                    break;
                default:
                    sb.append(current);
            }
        }
        return sb.toString();
    }

    private static void addIndentBlank(StringBuilder sb, int indent) {
        indent = indent + 4;
        for (int i = 0; i < indent; i++) {
            sb.append('\t');
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

    private static void writeJsonToFile(String responseJsonString, File jsonFile, String relativePath, boolean isPhp) {
        String phpResponse = "\t\t\t\t\"code\" : 1000,\n" +
                "\t\t\t\t\"bcode\" : 0,\n" +
                "\t\t\t\t\"message\" : \"\",\n" +
                "\t\t\t\t\"content\" : " + responseJsonString + ",\n" +
                "\t\t\t\t\"timeStamp\" : 1234567890\n";
        String netOrJavaResponse = "\t\t\t\"statusCode\" : 200,\n" +
                "\t\t\t\t\"success\" : true,\n" +
                "\t\t\t\t\"message\" : \"\",\n" +
                "\t\t\t\t\"identity\" : \"\",\n" +
                "\t\t\t\t\"data\" : " + responseJsonString + ",\n" +
                "\t\t\t\t\"timeStamp\" : 1234567890\n";
        String responseStr;
        if (isPhp) {
            responseStr = phpResponse;
        } else {
            responseStr = netOrJavaResponse;
        }
        String jsonStr = "[\n" +
                "\t{\n" +
                "\t\t\"request\" : {\n" +
                "\t\t\t\"method\" : \"get\",\n" +
                "\t\t\t\"uri\" : \"" + relativePath + "\"\n" +
                "\t\t},\n" +
                "\t\t\"response\" : {\n" +
                "\t\t\t\"json\" : {\n" +
                responseStr +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "]";

        try {
            FileWriter fileWriter = new FileWriter(jsonFile);
            fileWriter.write(jsonStr);
            fileWriter.close();
        } catch (Exception e) {
            System.out.print("e=> " + e.toString());
            e.printStackTrace();
        }
    }

    private static boolean deleteFile(String className, String classLocation) {
        boolean result = true;
        File dir = new File(classLocation);
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.getName().contains(className) && file.getName().contains(".class")) {
                result = result & file.delete();
            }
        }
        return result;
    }

    private static void includeToServer(String relativePath) {
        relativePath = relativePath.substring(1);
        String includeString = "\t{\n" +
                "\t\t\"include\" : \"" + relativePath + ".json\"\n" +
                "\t}";
        File file = new File("./server.json");
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String fileContent = "";
            String line;
            boolean justAddIncludeString = false;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("[")) {
                    fileContent = fileContent + line + "\n" + includeString;
                    justAddIncludeString = true;
                    continue;
                }
                if (justAddIncludeString && line.contains("{")) {
                    fileContent = fileContent + ",\n" + line;
                } else {
                    fileContent = fileContent + "\n" + line;
                }
                justAddIncludeString = false;
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(fileContent);
            fileWriter.close();
        } catch (Exception e) {
            System.out.println("includeToServer e=> " + e.toString());
            e.printStackTrace();
        }
    }
}
