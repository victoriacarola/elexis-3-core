package ch.elexis.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.elexis.core.services.IModelService;
import ch.elexis.core.services.IQuery;
import ch.elexis.core.services.IQuery.COMPARATOR;
import ch.elexis.core.types.ArticleTyp;
import ch.elexis.core.utils.OsgiServiceUtil;

public class TypedArticleTest {
	private IModelService modelSerice;
	
	@Before
	public void before(){
		modelSerice = OsgiServiceUtil.getService(IModelService.class).get();
	}
	
	@After
	public void after(){
		OsgiServiceUtil.ungetService(modelSerice);
		modelSerice = null;
	}
	
	@Test
	public void create(){
		ITypedArticle article = modelSerice.create(ITypedArticle.class);
		assertNotNull(article);
		assertTrue(article instanceof ITypedArticle);
		
		article.setName("test article");
		article.setCode("123456789");
		article.setTyp(ArticleTyp.EIGENARTIKEL);
		article.setGtin("0000001111111");
		article.setPackageUnit(12);
		article.setSellingUnit(12);
		modelSerice.save(article);
		
		Optional<ITypedArticle> loaded = modelSerice.load(article.getId(), ITypedArticle.class);
		assertTrue(loaded.isPresent());
		assertFalse(article == loaded.get());
		assertEquals(article, loaded.get());
		assertEquals(article.getCode(), loaded.get().getCode());
		assertEquals(article.getTyp(), loaded.get().getTyp());
		assertEquals(article.getGtin(), loaded.get().getGtin());
		assertEquals(article.getPackageUnit(), loaded.get().getPackageUnit());
		assertEquals(article.getSellingUnit(), loaded.get().getSellingUnit());
		
		modelSerice.remove(article);
	}
	
	@Test
	public void query(){
		ITypedArticle article = modelSerice.create(ITypedArticle.class);
		article.setName("test article");
		article.setCode("123456789");
		article.setTyp(ArticleTyp.EIGENARTIKEL);
		article.setGtin("0000001111111");
		article.setPackageUnit(12);
		article.setSellingUnit(12);
		modelSerice.save(article);
		
		ITypedArticle article1 = modelSerice.create(ITypedArticle.class);
		article1.setName("test article 1");
		article1.setCode("987654321");
		article1.setTyp(ArticleTyp.EIGENARTIKEL);
		article1.setGtin("1111112222222");
		article1.setPackageUnit(24);
		article1.setSellingUnit(24);
		modelSerice.save(article1);
		
		IQuery<ITypedArticle> query = modelSerice.getQuery(ITypedArticle.class);
		query.and(ModelPackage.Literals.IARTICLE__GTIN, COMPARATOR.EQUALS, "0000001111111");
		List<ITypedArticle> existing = query.execute();
		assertNotNull(existing);
		assertFalse(existing.isEmpty());
		assertEquals(article, existing.get(0));
		
		query = modelSerice.getQuery(ITypedArticle.class);
		query.and(ModelPackage.Literals.ITYPED_ARTICLE__TYP, COMPARATOR.EQUALS,
			ArticleTyp.EIGENARTIKEL);
		existing = query.execute();
		assertNotNull(existing);
		assertFalse(existing.isEmpty());
		assertEquals(2, existing.size());
		
		modelSerice.remove(article);
		modelSerice.remove(article1);
	}
}
