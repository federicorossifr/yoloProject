package it.unipi.ing.mim.deep.tools;

import it.unipi.ing.mim.deep.DetailedImage;
import it.unipi.ing.mim.deep.ImgDescriptor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Output {

	public static final int COLUMNS = 5;

	public static String generateHTML(List<ImgDescriptor> ids, String baseURI) {
		String html = "<html>\n<body>\n<table align='center'>\n";

		for (int i = 0; i < ids.size(); i++) {
			System.out.println(i + " - " + (float) ids.get(i).getDist() + "\t" + ids.get(i).getId() );

			if (i % COLUMNS == 0) {
				if (i != 0)
					html += "</tr>\n";
				html += "<tr>\n";
			}
			html += "<td>";
			html += "<div style='position: relative;'>";
			html += "<img align='center' border='0' title='" + ids.get(i).getId() + ", dist: "
			        + ids.get(i).getDist() + "' src='" + baseURI + ids.get(i).getId() + "'>";

			int bb_index = ids.get(i).getBoundingBoxIndex();
			if(bb_index != -1) {
				DetailedImage dimg;
				try {dimg = new DetailedImage(ids.get(i).getId());} catch(IOException e) { return "DetailedImage load error"; }
				int [] bb_coord = dimg.getBoundingBoxByIndex(bb_index);
				int left=bb_coord[0], right=bb_coord[1], top=bb_coord[2], bottom=bb_coord[3];
				int width = Math.abs(right-left);
				int height = Math.abs(bottom-top);
				html+="<div class='box' "
						+ "style='position: absolute; top: "+ top +"px;left: "+ left +"px;"
						+ "width: "+ width +"px;height: "+ height +"px;border: 2px solid #c02020;background-color: transparent;'>"
						+ "</div>";
			}

			html += "</td>\n";
		}
		if (ids.size() != 0)
			html += "</tr>\n";
		html += "</table>\n</body>\n</html>";

		return html;
	}

	public static void toHTML(List<ImgDescriptor> ids, String baseURI, File outputFile) {
		String html = generateHTML(ids, baseURI);
		
		try {
	        string2File(html, outputFile);
			System.out.print("html generated");
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
	}

	private static void string2File(String text, File file) throws IOException {
		try (FileWriter fileWriter = new FileWriter(file)) {
			fileWriter.write(text);
		}
	}
}
