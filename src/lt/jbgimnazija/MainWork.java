package lt.jbgimnazija;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/* atranda visu mokiniu link'us i ju tvarkarascius taip pat tu mokiniu vardus --- findNamesAndLinks()
 * atranda pamoku uzkodavimus pvz: 4_BiolLyg2:Biologiniai lygmenys ---- findClasses()
 */

public class MainWork {

	// mokiniu linkai ir vardai
	List<PairForList> info = new ArrayList<PairForList>();
	//3def_Mat5A_f i Matematika
	Map<String, String> lessonMap = new HashMap<String, String>();
	//\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\
	public static void main(String[] args) {

		MainWork doWork = new MainWork();

		doWork.findNamesAndLinks();
		
		doWork.findClasses();
		doWork.createMap();		//pavercia pvz.: 3def_Mat5A_f i Matematika
		doWork.findAllShedules();

	}
	//\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\
	private void createMap() {
	
		try(BufferedReader readFile = new BufferedReader(new FileReader(new File("lessonsMap.txt")))) {

			String line;
			boolean possition = true;
			while((line = readFile.readLine()) != null){
				//System.out.println(line);
				if (possition){
					possition = false;
					continue;
				}
		
				String keyAndValue [] = line.split("\t");
				lessonMap.put(keyAndValue[0], keyAndValue[1]);
				//System.out.println(keyAndValue[0] + " " +  keyAndValue[1]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	// -----------------------------------------------------------------------------------------------------------//

	static final int FIRST_GOOD_LINE = 7;
	
	private void findAllShedules()  {
		
		final int DAYS_OF_WEEK = 6; //plus time of lessons
		final int LESSONS_PER_DAY = 9; //plus safety lines ~115
		
		for (PairForList pair : info) {
			//change to lower case than ąčęėįšųūž to normal and delete spaces
			String studentName = replaceChars(pair.getName().toLowerCase()).replace(" ", "");
			//if classes swap
			if (studentName.startsWith("1") || studentName.startsWith("2")){
				studentName = new StringBuffer(studentName).reverse().toString();
			}
			URL studentUrl = pair.getURL();
	
			LessonInfo schedule [][] = new LessonInfo[LESSONS_PER_DAY][DAYS_OF_WEEK];
			boolean taken [][] = new boolean[LESSONS_PER_DAY][DAYS_OF_WEEK];
			
			Elements el = null;
			try {
				Document doc = Jsoup.parse(studentUrl.openStream(), "iso-8859-13", studentUrl.toString());
				el = doc.select("td");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(studentUrl.toString());
				continue;
			}

			int x = 0, y = 0;
			for (int i = 0; i < el.size(); i++){
				if (i >= FIRST_GOOD_LINE){
					Element element = el.get(i);
					String lessonName = null;
					//System.out.println(element.select("b").text());
					if(element.select("b").text().isEmpty())
						lessonName = "";
					else{
						String gg  = element.select("b").text();
				
						if (!lessonMap.containsKey(gg))
							System.out.println(gg+ "P");
						lessonName = lessonMap.get(gg);
					}
					//System.out.println(lessonName);
					Vector<String> nrOfClass = getClass(element.text().toString());
					boolean doubleRow = element.hasAttr("rowspan");
					
					while(taken[x][y] != false){
						y++;
						if (y == DAYS_OF_WEEK){
							x++;
							y = 0;
						}
					}
					
					if (!doubleRow){
						if (nrOfClass.size() == 1)
							schedule[x][y] = new LessonInfo(lessonName, nrOfClass.elementAt(0));
						else
							schedule[x][y] = new LessonInfo(lessonName, "");
						taken[x][y] = true;
					}
					//double lesson
					else{
						//jei vienas kabineto numeris
						if (nrOfClass.size() == 1){
							schedule[x][y] = new LessonInfo(lessonName, nrOfClass.elementAt(0));
							schedule[x+1][y] = new LessonInfo(lessonName, nrOfClass.elementAt(0));
						}
						//jei du kabineto numeriai
						else if (nrOfClass.size() == 2){
							schedule[x][y] = new LessonInfo(lessonName, nrOfClass.elementAt(0));
							schedule[x+1][y] = new LessonInfo(lessonName, nrOfClass.elementAt(1));
						}
						else{
							schedule[x][y] = new LessonInfo(lessonName, "");
							schedule[x+1][y] = new LessonInfo(lessonName, "");
							
						}
						taken[x][y] = taken[x+1][y] = true;
					}
						
				}
					
			}
			
		////////////////////////////////PRINT DATA OF STUDENT TO SPECIFIC FILE////////////////////////////////////
			PrintWriter writeToFile = null;
			try {
				writeToFile = new PrintWriter(new File(studentName+".txt"), "utf-8");
				for(int i = 1; i < DAYS_OF_WEEK; i++){
					for (int j = 0; j < LESSONS_PER_DAY-1; j++){
						writeToFile.write(schedule[j][i].getInfo());
					}
				}
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}finally{
				if (writeToFile!=null)
					writeToFile.close();
			}

			///////////////////////////////////////////////////////////////////////////////////////////////////////////////
		}
		
	}
	
	private Vector<String> getClass(String string) {
		final int LENGTH_OF_CLASS = 3;
		Vector<String> vector = new Vector<String>();
		for (int i = 0; i < string.length()-LENGTH_OF_CLASS + 1; i++){
			String sub = string.substring(i, i+LENGTH_OF_CLASS);
			if(isStringNumber(sub)){
				 vector.add(sub);
			}
		}
		return vector;
	}
	private boolean isStringNumber(String substring) {
		try{
			Integer.parseInt(substring);
		}catch(NumberFormatException e){
			return false;
		}
		return true;
	}

	private String replaceChars(String nameText){
		nameText = nameText.replace("ą", "a");
		nameText = nameText.replace("č", "c");
		nameText = nameText.replace("ę", "e");
		nameText = nameText.replace("ė", "e");
		nameText = nameText.replace("į", "i");
		nameText = nameText.replace("ų", "u");
		nameText = nameText.replace("ū", "u");
		nameText = nameText.replace("š", "s");
		nameText = nameText.replace("ž", "z");
		return nameText;
	}
	
	private class LessonInfo{
		
		String lesson, nrOfClass;
		
		public LessonInfo(String lesson, String nrOfClass){

			this.lesson = lesson;
			this.nrOfClass = nrOfClass;	

		}
		public String getInfo(){
			return lesson + "^" + nrOfClass + "\n";
		}
		
	}
	// -----------------------------------------------------------------------------------------------------------//

	static final String CLASSES = "http://jbgimnazija.lt/Grupes15_1pusm/grupes_1pusm.htm";

	static final int CLASSES_START = 17;
	static final int CLASSES_END = 808;

	private void findClasses() {

		try {
			File classesFile = new File("allClasses.txt");

			PrintWriter writeToClasses = new PrintWriter(classesFile);

			BufferedReader readClasses = new BufferedReader(
					new InputStreamReader(new URL(CLASSES).openStream(),
							"iso-8859-13"));
			String line;
			int linePossition = 1;
			while ((line = readClasses.readLine()) != null) {
				if (linePossition >= CLASSES_START
						&& linePossition <= CLASSES_END) {
					Document doc = Jsoup.parse(line);
					String lesson = doc.text().toString().replace(":", "\t");
					if (!lesson.isEmpty()) {
						writeToClasses.write(lesson + "\n");

					}
				}
				linePossition++;
			}
			writeToClasses.close();
			readClasses.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// dabar failo allClasses.txt visa turini perkeli i exel ir sutvarkai
		// pavadinimus
		// kaip tai turi atrodyti paziurek i .....txt

	}

	// -----------------------------------------------------------------------------------------------------------//

	static final String URL_LINK = "http://jbgimnazija.lt/TvarkPJBG1pusm_2/tvarkara+tis_1pusm.htm";
	static final String BASE_URL = "http://jbgimnazija.lt/TvarkPJBG1pusm_2/";

	static final int STARTING_LINE_3_4 = 184;
	static final int ENDING_LINE_3_4 = 711;
	
	static final int STARTING_LINE_1_2 = 18;
	static final int ENDING_LINE_1_2 = 33;
	
	private void findNamesAndLinks() {

		try {
			// file in PC
			File file = new File("namesonly.txt");
			PrintWriter writeNames = new PrintWriter(file, "utf-8");

			// file from internet
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new URL(URL_LINK).openStream(), "iso-8859-13"));

			// reading file from internet and writing to file on PC
			String line;
			int numberOfLine = 1;
			while ((line = reader.readLine()) != null) {
				if ( (numberOfLine >= STARTING_LINE_3_4 && numberOfLine <= ENDING_LINE_3_4) ||
						(numberOfLine >= STARTING_LINE_1_2 && numberOfLine <= ENDING_LINE_1_2) ){

					// from html to text using Jsoup
					Document docLine = Jsoup.parse(line);

					Element hyperLinkTag = docLine.select("a").first();
					// if line has hyperLink tag
					if (hyperLinkTag != null) {
					//	System.out.println(hyperLinkTag);
						String linkToStudent = BASE_URL
								+ hyperLinkTag.attr("href");
						String cleanText = hyperLinkTag.text();
						String realName = cleanText.substring(cleanText
								.indexOf(":") + 1).replace("-", " ").replaceAll("\\s+", " ");

						PairForList pair = new PairForList(realName, new URL(
								linkToStudent));
						info.add(pair);

						writeNames.write(realName + '\n');
					}

				}
				numberOfLine++;
			}
			reader.close();
			writeNames.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
	}

	// -----------------------------------------------------------------------------------------------------------//

	// like pair<> in c++
	private class PairForList {
		URL url;
		String name;

		public PairForList(String name, URL url) {
			this.url = url;
			this.name = name;
		}

		public URL getURL() {
			return url;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return "PairForList [url=" + url + ", name=" + name + "]\n";
		}
	}

}
