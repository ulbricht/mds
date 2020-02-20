package org.datacite.mds.domain;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import javax.validation.GroupSequence;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.log4j.Logger;
import org.datacite.mds.util.Constants;
import org.datacite.mds.util.Utils;
import org.datacite.mds.validation.constraints.Doi;
import org.datacite.mds.validation.constraints.MatchDoiPrefix;
import org.datacite.mds.validation.constraints.MatchDomain;
import org.datacite.mds.validation.constraints.URL;
import org.datacite.mds.validation.constraints.Unique;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Table;

@RooJavaBean
@RooToString
@RooEntity(finders = { "findDatasetsByDoiEquals" })
@Doi
@MatchDoiPrefix(groups = Dataset.SecondLevelConstraint.class)
@MatchDomain(groups = Dataset.SecondLevelConstraint.class)
@Unique(field = "doi")
@GroupSequence({ Dataset.class, Dataset.SecondLevelConstraint.class })
@Table(name="dataset")
public class Dataset {
    
    private static Logger log4j = Logger.getLogger(Dataset.class);

    @NotNull
    @Column(unique = true)
    @Size(max = 255)
    private String doi;

    @NotNull
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_ref_quality")
    private Boolean isRefQuality = false;

    @Min(100L)
    @Max(510L)
    @Column(name = "last_landing_page_status")
    private Integer lastLandingPageStatus;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(iso = ISO.DATE_TIME)
    @Column(name = "last_landing_page_status_check")
    private Date lastLandingPageStatusCheck;

    @Column(name = "last_metadata_status")
    private String lastMetadataStatus;

    @NotNull
    @ManyToOne(targetEntity = Datacentre.class)
    @JoinColumn(name="datacentre")
    private Datacentre datacentre;

//    @Transient
    @URL
    private String url;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(iso = ISO.DATE_TIME)
    private Date created;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(iso = ISO.DATE_TIME)
    private Date updated;
    
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(iso = ISO.DATE_TIME)
    private Date minted;

    @Transient
    public Metadata getLatestMetadata() {
        return Metadata.findLatestMetadatasByDataset(this);
    }

    private static TypedQuery<Dataset> queryDatasetsByAllocatorOrDatacentre(AllocatorOrDatacentre user) {
        EntityManager em = entityManager();
        String hql;
        if (user instanceof Datacentre) {
            hql = "SELECT dataset FROM Dataset AS dataset WHERE dataset.datacentre = :user ORDER BY dataset.updated DESC";
        } else {
            hql = "SELECT dataset FROM Dataset AS dataset WHERE dataset.datacentre.allocator = :user ORDER BY dataset.updated DESC";
        }
        TypedQuery<Dataset> q = em.createQuery(hql, Dataset.class);
        q.setParameter("user", user);
        return q;
    }

    public static List<Dataset> findDatasetEntriesByAllocatorOrDatacentre(AllocatorOrDatacentre user, int firstResult, int maxResults) {
        TypedQuery<Dataset> q = queryDatasetsByAllocatorOrDatacentre(user);
        return q.setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

    public static List<Dataset> findDatasetsByAllocatorOrDatacentre(AllocatorOrDatacentre user) {
        TypedQuery<Dataset> q = queryDatasetsByAllocatorOrDatacentre(user);
        return q.getResultList();
    }

    public static long countDatasetsByAllocatorOrDatacentre(AllocatorOrDatacentre user, String prefix) {
        EntityManager em = entityManager();
        String hql;
        if (user instanceof Allocator)
            hql = "SELECT COUNT(*) FROM Dataset AS dataset WHERE dataset.datacentre.allocator = :user";
        else
            hql = "SELECT COUNT(*) FROM Dataset AS dataset WHERE dataset.datacentre = :user";
        
        if (prefix != null)
            hql += " AND dataset.doi like :prefix";
        
        TypedQuery<Long> q = em.createQuery(hql, Long.class);
        q.setParameter("user", user);
        
        if (prefix != null)
            q.setParameter("prefix", prefix + "%");
        
        return q.getSingleResult();
    }

    public static long countDatasetsByAllocatorOrDatacentre(AllocatorOrDatacentre user) {
        return countDatasetsByAllocatorOrDatacentre(user, null);
    }

    public static long countTestDatasetsByAllocatorOrDatacentre(AllocatorOrDatacentre user) {
        return countDatasetsByAllocatorOrDatacentre(user, Constants.TEST_PREFIX);
    }

    
    @Transactional
    public void persist() {
        Date date = new Date();
        setCreated(date);
        setUpdated(date);
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.persist(this);
    }

    @Transactional
    public Dataset merge() {
        setUpdated(new Date());
        if (this.entityManager == null) this.entityManager = entityManager();
        Dataset merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }
    
    /**
     * retrieve a dataset by 
     * 
     * @param doi
     *            of an dataset
     * @return dataset with the given doi or null if no such dataset
     *         exists
     */
    public static Dataset findDatasetByDoi(String doi) {
        doi = Utils.normalizeDoi(doi);
        if (doi == null) {
            return null;
        }
        try {
            log4j.trace("search for '" + doi + "'");
            Dataset dataset = findDatasetsByDoiEquals(doi).getSingleResult();
            log4j.trace("found '" + doi + "'");
            return dataset;
        } catch (Exception e) {
            log4j.trace("no dataset found");
            return null;
        }
    }
    
    public static List<Dataset> findDatasetsByPrefix(String prefix) {
        EntityManager em = entityManager();
        String hql = "select dataset from Dataset AS dataset WHERE dataset.doi like :prefix";
        TypedQuery<Dataset> query = em.createQuery(hql, Dataset.class);
        query.setParameter("prefix", prefix + "%");
        return query.getResultList();
    }

    public void setIgsn(String doi) {
        setDoi(doi);
    }

    public void setDoi(String doi) {
        this.doi = Utils.normalizeDoi(doi);
    }
    
    @Override
    public String toString() {
        return getDatacentre().getSymbol() + ":" + getDoi() + " (id=" + getId() + ")";
    }
    
    public interface SecondLevelConstraint {};
}
