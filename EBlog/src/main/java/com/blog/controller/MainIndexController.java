package com.blog.controller;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.blog.model.BllArticle;
import com.blog.model.BllCommont;
import com.blog.utils.DateBindController;
import com.blog.utils.DbAction;
import com.blog.utils.HibernateUtils;
import com.blog.utils.JsonHelper;

@Controller
@RequestMapping("/MainIndex")
public class MainIndexController extends DateBindController {

	private static Logger logger = Logger.getLogger(MainIndexController.class);

	int pCount = 15;// 每页显示记录数目

	// 生成分页按钮
	@RequestMapping("/getArticlePage")
	@ResponseBody
	public Map<String, Object> getArticlePage(HttpServletRequest request,
			@RequestParam(value = "action", required = true) String action) {

		// 获取程序部署路径，使用字符串${ctxPath}，传到前台还是${ctxPath}，不能获取路径
		// String url = request.getContextPath() +
		// "/MainIndex/getallArticle.do";
		String url = request.getContextPath() + action;
		String strSql = "select count(*) from bll_article ";

		boolean isType = false;// 判断是否是点击了分类，分类则带有类别参数
		if (url.contains("typeid=")) {
			strSql = "select count(*) from bll_article where typeid=" + url.split("=")[1];
			isType = true;
		}

		int toalCount = (int) HibernateUtils.queryOne(strSql);// 总数
		int page = (toalCount % pCount == 0) ? (toalCount / pCount) : (toalCount / pCount + 1);// 总页数
		StringBuffer strBPageHtml = new StringBuffer();
		strBPageHtml.append("<p>");
		strBPageHtml.append("共");
		strBPageHtml.append(page);
		strBPageHtml.append("页&nbsp;");
		for (int i = 1; i < page + 1; i++) {
			if (i == 1) {
				strBPageHtml.append("<a class='aPageDisable' href='javascript:void(0);'");
			} else {
				strBPageHtml.append("<a href='javascript:void(0);'");
			}

			strBPageHtml.append(" onclick=\"getArticle('");
			strBPageHtml.append(url);
			if (isType) {
				strBPageHtml.append("&page=");
			} else {
				strBPageHtml.append("?page=");
			}

			strBPageHtml.append(i);
			strBPageHtml.append("')\">");
			strBPageHtml.append(i);
			strBPageHtml.append("</a>&nbsp;");
		}
		strBPageHtml.append("</p>");

		return JsonHelper.getModel(strBPageHtml.toString());
	}

	// 获取分类
	@RequestMapping("/getCategory")
	@ResponseBody
	public Map<String, Object> getCategory(HttpServletRequest request) throws SQLException {

		String strSql = "select b.id typeid,b.typename,count(typeid) countn from bll_article a right join bll_articletype b on a.typeid=b.id group by b.id,b.typename order by countn desc";

		StringBuffer strBCategory = new StringBuffer();
		strBCategory.append("<ul>");

		ResultSet rs = DbAction.getQuery(strSql);
		while (rs.next()) {
			strBCategory.append("<li><a onClick='addTypeMenu(\"");
			strBCategory.append(rs.getString("typename"));
			strBCategory.append("\",\"");
			strBCategory.append(rs.getString("typeid"));
			strBCategory.append("\")");
			strBCategory.append("' href=\"#\" >");
			strBCategory.append(rs.getString("typename"));
			strBCategory.append("(");
			strBCategory.append(rs.getString("countn"));
			strBCategory.append(")");
			strBCategory.append("</a></li>");
		}

		strBCategory.append("</ul>");

		return JsonHelper.getModel(strBCategory.toString());
	}

	// 按照文章分类读取该分类下的文章
	@RequestMapping("/getArticleByType")
	@ResponseBody
	public Map<String, Object> getArticleByType(Model model,
			@RequestParam(value = "typeid", required = true) String typeid,
			@RequestParam(value = "page", required = true) String page) {
		int pageNum = 1;// 当前页
		if (!page.isEmpty() && page != "") {
			pageNum = Integer.parseInt(page);
		}
		String pageSql = " limit " + (pageNum - 1) * pCount + "," + pCount;

		List<BllArticle> list = HibernateUtils.queryListParam(BllArticle.class,
				"select * from bll_article where typeid=" + typeid + pageSql);

		List<BllArticle> newList = removeArtilceHtml(list);
		model.addAttribute("dto", newList);

		return JsonHelper.getModelMap(newList);
	}

	/*
	 * // 读取所有的文章，按照时间先后顺序排列
	 * 
	 * @RequestMapping("/getallArticle")
	 * 
	 * @ResponseBody public Map<String, Object> getallArticleList() {
	 * List<BllArticle> list = HibernateUtils.queryListParam(BllArticle.class,
	 * "select * from bll_article order by CreateTime desc"); List<BllArticle>
	 * newList = removeArtilceHtml(list); return
	 * JsonHelper.getModelMap(newList); }
	 */

	// 分页读取文章，按照时间先后顺序排列
	@RequestMapping("/getallArticle")
	@ResponseBody
	public Map<String, Object> getallArticleList(@RequestParam(value = "page", required = true) String page) {
		int pageNum = 1;// 当前页
		if (!page.isEmpty() && page != "") {
			pageNum = Integer.parseInt(page);
		}
		String pageSql = " limit " + (pageNum - 1) * pCount + "," + pCount;

		List<BllArticle> list = HibernateUtils.queryListParam(BllArticle.class,
				"select * from bll_article order by CreateTime desc" + pageSql);

		List<BllArticle> newList = removeArtilceHtml(list);
		return JsonHelper.getModelMap(newList);
	}

	// 按照阅读量排列
	@RequestMapping("/getArticleRead")
	@ResponseBody
	public Map<String, Object> getArticleRead(@RequestParam(value = "page", required = true) String page) {
		int pageNum = 1;// 当前页
		if (!page.isEmpty() && page != "") {
			pageNum = Integer.parseInt(page);
		}
		String pageSql = " limit " + (pageNum - 1) * pCount + "," + pCount;

		List<BllArticle> list = HibernateUtils.queryListParam(BllArticle.class,
				"select * from bll_article order by ReadCount desc" + pageSql);

		List<BllArticle> newList = removeArtilceHtml(list);
		return JsonHelper.getModelMap(newList);
	}

	// 按照评论量排列
	@RequestMapping("/getArticleCommit")
	@ResponseBody
	public Map<String, Object> getArticleCommit(@RequestParam(value = "page", required = true) String page) {
		int pageNum = 1;// 当前页
		if (!page.isEmpty() && page != "") {
			pageNum = Integer.parseInt(page);
		}
		String pageSql = " limit " + (pageNum - 1) * pCount + "," + pCount;

		List<BllArticle> list = HibernateUtils.queryListParam(BllArticle.class,
				"select * from bll_article order by ComCount desc" + pageSql);

		List<BllArticle> newList = removeArtilceHtml(list);
		return JsonHelper.getModelMap(newList);
	}

	// 按照推荐量排列
	@RequestMapping("/getArticleSuggest")
	@ResponseBody
	public Map<String, Object> getArticleSuggest(@RequestParam(value = "page", required = true) String page) {
		int pageNum = 1;// 当前页
		if (!page.isEmpty() && page != "") {
			pageNum = Integer.parseInt(page);
		}
		String pageSql = " limit " + (pageNum - 1) * pCount + "," + pCount;

		List<BllArticle> list = HibernateUtils.queryListParam(BllArticle.class,
				"select * from bll_article order by SuggestCount desc" + pageSql);

		List<BllArticle> newList = removeArtilceHtml(list);
		return JsonHelper.getModelMap(newList);
	}

	// 读取某个文章的评论数
	@RequestMapping("/getSingleComm")
	@ResponseBody
	public Map<String, Object> getSingleComm(String id) {
		int count = (int) HibernateUtils.queryOne("select count(*) from bll_commont where ArticleID='" + id + "'");

		return JsonHelper.getModel(count);
	}

	// 通过文章id，读取文章信息和评论
	@RequestMapping("/getDetailById")
	public String getDetailById(Model model, @RequestParam(value = "id", required = true) String id) {
		// 读取文件详细内容
		BllArticle article = (BllArticle) HibernateUtils.findById(BllArticle.class, id);
		model.addAttribute("artdto", article);

		// 读取该文章的评论
		List<BllCommont> comList = HibernateUtils.queryListParam(BllCommont.class,
				"select * from bll_commont where ArticleID='" + id + "' order by comtime asc");
		model.addAttribute("comList", comList);

		return "blog/article/view";
	}

	// 读取该用户的所有博客
	@RequestMapping("/getArticleByCreateBy")
	public String getArticleByCreateBy(Model model, @RequestParam(value = "user", required = true) String user) {
		List<BllArticle> list = HibernateUtils.queryListParam(BllArticle.class,
				"select * from bll_article where createBy='" + user + "' order by CreateTime desc");

		List<BllArticle> newList = removeArtilceHtml(list);
		model.addAttribute("dto", newList);

		return "blog/article/viewlistuser";
	}

	/**
	 * 用于首页列表展示时，只需要列出部分内容，且不需要展示样式。故过滤掉样式及空格
	 * 
	 * @param articles源文章list
	 * @return 过滤后的文章list
	 */
	private List<BllArticle> removeArtilceHtml(List<BllArticle> articles) {
		List<BllArticle> newarticle = null;
		if (articles != null && articles.size() > 0) {
			newarticle = new ArrayList<BllArticle>();
			for (int i = 0; i < articles.size(); i++) {
				String artc = articles.get(i).getContent().replaceAll("<.*?>", "").replace("&nbsp;", "");
				if (artc != null && artc.length() > 200) {
					artc = artc.substring(0, 200) + " ... ";
				}
				articles.get(i).setContent(artc);
				newarticle.add(i, articles.get(i));
			}
		} else {
			return articles;
		}
		return newarticle;
	}
}
