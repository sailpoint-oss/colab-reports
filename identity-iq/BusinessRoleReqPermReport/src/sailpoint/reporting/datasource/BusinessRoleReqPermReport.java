/**
 * A custom SailPoint BundleIQ report used for easily reporting on all required and permitted roles.
 */
package sailpoint.reporting.datasource;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.api.IncrementalObjectIterator;
import sailpoint.api.SailPointContext;
import sailpoint.object.*;
import sailpoint.task.Monitor;
import sailpoint.tools.GeneralException;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class BusinessRoleReqPermReport implements JavaDataSource {
  private IncrementalObjectIterator<Bundle> bundles = null;
  private Bundle currentBundle = null;
  private Monitor monitor;
  private SailPointContext context;
  private String defLang = "en_US";
  private QueryOptions baseQueryOptions;
  private Integer startRow;
  private Integer pageSize;
  private Object[] currentRow;
  private Iterator<Object[]> bundleIterator;
  private Iterator<Bundle> requiredIterator;
  private Iterator<Bundle> permittedIterator;
  private Bundle requiredBundle;
  private Bundle permittedBundle;
  private final Log log = LogFactory.getLog(getClass().getName());

  @Override
  public Object getFieldValue(String field) throws GeneralException {

    String result = "";

    switch (field) {
      case "name":
        result = currentBundle.getName();
        break;
      case "displayName":
        result = currentBundle.getDisplayName();
        break;
      case "IT":
        if (requiredBundle != null) {
          result = requiredBundle.getName();
        } else {
          if (permittedBundle != null) {
            result = permittedBundle.getName();
          }
        }
        break;
      case "ITDisplayName":
        if (requiredBundle != null) {
          result = requiredBundle.getDisplayName();
        } else {
          if (permittedBundle != null) {
            result = permittedBundle.getDisplayName();
          }
        }
        break;
      case "reqPerm":
        if (requiredBundle != null) {
          result = "Required";
        } else {
          if (permittedBundle != null) {
            result = "Permitted";
          }
        }
        break;
      case "selector":
        IdentitySelector identitySelector = this.currentBundle.getSelector();
        if (null != identitySelector) {
          result = identitySelector.generateSummary();
        } else {
          result = "";
        }
        break;
      case "owner":
        Identity ownerIdentity = this.currentBundle.getOwner();
        if (null != ownerIdentity) {
          result = ownerIdentity.getName();
        } else {
          result = "";
        }
        break;
      case "description":
        result = currentBundle.getDescription(this.defLang);
        break;
      default:

    }
    if (log.isDebugEnabled()) log.debug("getFieldValue " + field + "=" + result);

    return result;
  }

  @Override
  public Object getFieldValue(JRField jRField) throws JRException {
    try {
      return getFieldValue(jRField.getName());
    } catch (GeneralException e) {
      throw new JRException(e);
    }
  }

  @Override
  public void initialize(SailPointContext context, LiveReport report, Attributes<String, Object> attribs, String groupBy, List<Sort> sort) throws GeneralException {

    if (log.isDebugEnabled()) log.debug("BusinessRoleReqPermReport initialize");
    this.context = context;
    this.baseQueryOptions = new QueryOptions();
    this.baseQueryOptions.setCloneResults(true);

    this.baseQueryOptions.addFilter(Filter.eq("type", "business"));

    if (attribs.containsKey("disabled")) {
      if ("true".equalsIgnoreCase((String) attribs.get("disabled"))) {
        this.baseQueryOptions.addFilter(Filter.eq("disabled", true));
      } else {
        this.baseQueryOptions.addFilter(Filter.ne("disabled", true));
      }
    }

    if (sort != null) {
      for (Sort sortItem : sort) {
        this.baseQueryOptions.addOrdering(
          sortItem.getField(),
          sortItem.isAscending()
        );
      }
    }
    if (groupBy != null) {
      this.baseQueryOptions.setGroupBys(
        Arrays.asList(new String[]{groupBy})
      );
    }
    Configuration configObj = Configuration.getSystemConfig();
    this.defLang = (String) configObj.get(Configuration.DEFAULT_LANGUAGE);
  }

  private void prepare() throws GeneralException {
    if (log.isDebugEnabled()) log.debug("prepare");
    QueryOptions ops = new QueryOptions(this.baseQueryOptions);
    ops.setCloneResults(true);
    if ((this.startRow != null) && (this.startRow.intValue() > 0)) {
      ops.setFirstRow(this.startRow.intValue());
    }
    if ((this.pageSize != null) && (this.pageSize.intValue() > 0)) {
      ops.setResultLimit(this.pageSize.intValue());
    }
    this.bundleIterator =
      this.context.search(
        Bundle.class,
        ops,
        Arrays.asList(new String[]{"id"})
      );
  }

  @Override
  public boolean next() throws JRException {
    if (log.isDebugEnabled()) log.debug("next");
    if (this.bundleIterator == null) {
      try {
        prepare();
      } catch (GeneralException e) {
        throw new JRException(e);
      }
    }
    if ((this.requiredIterator != null) && (this.requiredIterator.hasNext())) {
      this.requiredBundle = this.requiredIterator.next();
      this.permittedBundle = null;
      if (log.isDebugEnabled()) log.debug("next: return required bundle " + this.requiredBundle.getName());
      return true;
    }
    if ((this.permittedIterator != null) && (this.permittedIterator.hasNext())) {
      this.permittedBundle = this.permittedIterator.next();
      this.requiredBundle = null;
      if (log.isDebugEnabled()) log.debug("next: return permitted bundle " + this.permittedBundle.getName());
      return true;
    }
    while (this.bundleIterator.hasNext()) {
      this.currentRow = ((Object[]) this.bundleIterator.next());
      String bundleId = (String) this.currentRow[0];
      try {
        this.currentBundle = ((Bundle) this.context.getObjectById(Bundle.class, bundleId));
      } catch (GeneralException ex) {
        log.error("Can not find bundle with id=" + bundleId, ex);
      }
      if (log.isDebugEnabled())
        log.debug("Processing Bundle " + this.currentBundle.getName() + " (" + this.currentBundle.getDisplayName() + ")");

      List<Bundle> requiredList = this.currentBundle.getRequirements();
      if (null != requiredList) {
        this.requiredIterator = requiredList.iterator();
      } else {
        this.requiredIterator = null;
      }

      List<Bundle> permittedList = this.currentBundle.getPermits();
      if (null != permittedList) {
        this.permittedIterator = permittedList.iterator();
      } else {
        this.permittedIterator = null;
      }

      if ((this.requiredIterator != null) && (this.requiredIterator.hasNext())) {
        this.requiredBundle = this.requiredIterator.next();
        this.permittedBundle = null;
        return true;
      }
      if ((this.permittedIterator != null) && (this.permittedIterator.hasNext())) {
        this.permittedBundle = this.permittedIterator.next();
        this.requiredBundle = null;
        return true;
      }
    }
    return false;
  }

  @Override
  public void setLimit(int startRow, int pageSize) {
    this.startRow = Integer.valueOf(startRow);
    this.pageSize = Integer.valueOf(pageSize);
  }

  @Override
  public String getBaseHql() {
    return null;
  }

  @Override
  public QueryOptions getBaseQueryOptions() {
    return this.baseQueryOptions;
  }

  @Override
  public int getSizeEstimate() throws GeneralException {
    return this.context.countObjects(Bundle.class, this.baseQueryOptions);
  }

  @Override
  public void close() {
    // does nothing
  }

  @Override
  public void setMonitor(Monitor monitor) {
    this.monitor = monitor;
  }

}
