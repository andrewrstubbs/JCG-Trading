import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.regex.Pattern;
/**
 *Finds the first mention of an NYSE-listed company within some input text.
 *First searches for an NYSE stock ticker, then attempts to find some variation of a matching company
 *name as listed on the NYSE through regular expression 
 * 
 */
public class Preprocessor {
	private Connection conn;
	private static ArrayList<String[]> companylist = null;
	private static ArrayList<String> tickerlist = null;
	private static ArrayList<Integer> format = null;
	private String[] regex = {"^(C|c)o(\\.|m|mpany)?$", "^(C|c)o(\\.|r|rp|rp\\.|r|r\\.|rporation)?$", 
			"^(L|l)(td|td\\.|imited|mtd)$", "^(I|i)nc(\\.|orporated)?$", 
			"^(L|l)(\\.?(P|p)\\.?|imited (P|p)artnership)$", 
			"^(L|l)(imited (L|l)iability (C|c)ompany|(\\.)?(L|l)\\.?(C|c)\\.?)$", 
			"^(S|s)\\.?(a|A)\\.?$", "^(N|n)\\.?(V|v)\\.?$",};
	// possible tri data structure for speedup

	public Preprocessor() throws SQLException {
		conn = DriverManager.getConnection("jdbc:sqlserver://localhost\\sqlexpress;integratedSecurity=true;");
		tickerlist = new ArrayList<String>();
		companylist = new ArrayList<String[]>();
		format = new ArrayList<Integer>();
		getAllCompanyInfo();
//		for(int i = 0; i < companylist.size(); i++){
//			System.out.println(companylist.get(i)[0] + " " + companylist.get(i)[1]);
//		}
	}

	public void getAllCompanyInfo() {
		String update = "SELECT * FROM ProjectInfo.dbo.CompanyInfo";
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(update);
			while (rs.next()) {
				// results.get(count)[0] = Integer.toString(rs.getInt("ID"));
				tickerlist.add(rs.getString("Ticker"));
				//dont split this
				companylist.add(rs.getString("Name").split("\\s+"));
				format.add(rs.getInt("NameReq"));
			}

		} catch (SQLException e) {
			System.out.println("Failed to get Company Data");
		}

	}
	
	public String[] splitData(String data){
		String[] article = data.replaceAll("[^a-zA-Z ]", "").split("\\s+");
		return article;
	}
	
	//Combine these methods(edited and expurgated)
	public String[] editedArticle(String[] data){
		String[] article = data.clone();
		for(int i = 0; i < article.length; i++){
			for(int j = 0; j < regex.length; j++){
				if(Pattern.matches(regex[j], article[i])){
					article[i] = regex[j];
					break;
				}
			}
		}
		return article;
	}
	public String[] expurgatedArticle(String[] data){
		String[] article = data.clone();
		for(int i = 0; i < article.length; i++){
			for(int j = 0; j < regex.length; j++){
				if(Pattern.matches(regex[j], article[i])){
					article[i] = "";
					break;
				}
			}
		}
		return article;
		
	}
	// finds the first mentioned company (maybe most)
	public String search_company(String data) {
		String[] sData = splitData(data);
		String[] edData = editedArticle(sData);
		String[] exData = expurgatedArticle(sData);
		String[] rawData = data.split("\\s+");

				
		for (int i = 0; i < edData.length; i++) {
			if(rawData[i].equals("")){
				continue;
			}
			else if (!Character.isUpperCase(exData[i].charAt(0))) {
				continue;
			}
			else if(rawData[i].charAt(0) == '('){
				for(int x = 0; x < tickerlist.size(); x++){
					if(rawData[i].equals("(" + tickerlist.get(x) + ")" )){
						return tickerlist.get(x);
					}
					else if(rawData[i].equals("NYSE:") && rawData[i+1].equals(tickerlist.get(x)+ ")")){
						return tickerlist.get(x);
					}
					else if(rawData[i].equals("(NYSE:" + tickerlist.get(x)+ ")")){
						return tickerlist.get(x);
					}
				} 

			}
			for (int j = 0; j < companylist.size(); j++) {
				if(companylist.get(j).length > (edData.length - i)){
					continue;
				}
				
				switch(format.get(j)){
				case 0:
					boolean match = true;
					String[] company_name = companylist.get(j);
					for (int k = 0; k < Math.min(company_name.length, 2); k++) {
						if (exData[i + k].equals(company_name[k])) {
						} else {
							match = false;
							break;
						}
					}
					if (match) {
						return tickerlist.get(j);
					}
					break;
				case 1:
					boolean match2 = true;
					String[] company_name2 = companylist.get(j);
					for (int k = 0; k < company_name2.length; k++) {
						if (Pattern.matches(edData[i + k], company_name2[k])){
						} else {
							match2 = false;
							break;
						}
					}
					if (match2) {
						return tickerlist.get(j);
					}
					break;
					
				case 2:
					boolean match3 = true;
					String[] company_name3 = companylist.get(j);
					for (int k = 0; k < company_name3.length; k++) {
						if (exData[i + k].equals(company_name3[k])) {
						} else {
							match3 = false;
							break;
						}
					}
					if (match3) {
						return tickerlist.get(j);
					}
					
					break;
				case 3:
					boolean match4 = true;
					String[] company_name4 = companylist.get(j);
					for (int k = 0; k < company_name4.length; k++) {
						if (Pattern.matches(exData[i + k], company_name4[k])){
						} else {
							match4 = false;
							break;
						}
					}
					if (match4) {
						return tickerlist.get(j);
					}
					break;
				}
			}
		}
		return null;
	}
}
