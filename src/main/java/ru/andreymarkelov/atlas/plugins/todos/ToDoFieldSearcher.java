package ru.andreymarkelov.atlas.plugins.todos;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.Collections;
import java.util.Set;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.JiraDataTypes;
import com.atlassian.jira.bc.issue.search.QueryContextConverter;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.customfields.CustomFieldSearcher;
import com.atlassian.jira.issue.customfields.searchers.AbstractInitializationCustomFieldSearcher;
import com.atlassian.jira.issue.customfields.searchers.CustomFieldSearcherClauseHandler;
import com.atlassian.jira.issue.customfields.searchers.SimpleCustomFieldSearcherClauseHandler;
import com.atlassian.jira.issue.customfields.searchers.information.CustomFieldSearcherInformation;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.indexers.FieldIndexer;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.LuceneFieldSorter;
import com.atlassian.jira.issue.search.searchers.information.SearcherInformation;
import com.atlassian.jira.issue.search.searchers.renderer.SearchRenderer;
import com.atlassian.jira.issue.search.searchers.transformer.SearchInputTransformer;
import com.atlassian.jira.issue.statistics.TextFieldSorter;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.ActualValueCustomFieldClauseQueryFactory;
import com.atlassian.jira.jql.util.IndexValueConverter;
import com.atlassian.jira.jql.util.JqlSelectOptionsUtil;
import com.atlassian.jira.jql.util.SimpleIndexValueConverter;
import com.atlassian.jira.jql.validator.ExactTextCustomFieldValidator;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.query.operator.Operator;
import com.atlassian.util.concurrent.atomic.AtomicReference;

public class ToDoFieldSearcher extends AbstractInitializationCustomFieldSearcher implements CustomFieldSearcher {
    private final FieldVisibilityManager fieldVisibilityManager;
    private final JqlOperandResolver jqlOperandResolver;
    private final CustomFieldInputHelper customFieldInputHelper;

    private volatile CustomFieldSearcherInformation searcherInformation;
    private volatile SearchInputTransformer searchInputTransformer;
    private volatile SearchRenderer searchRenderer;
    private volatile CustomFieldSearcherClauseHandler customFieldSearcherClauseHandler;

    public ToDoFieldSearcher(final JqlOperandResolver jqlOperandResolver, final CustomFieldInputHelper customFieldInputHelper) {
        this(jqlOperandResolver, customFieldInputHelper, ComponentAccessor.getComponent(FieldVisibilityManager.class));
    }

    public ToDoFieldSearcher(JqlOperandResolver jqlOperandResolver, CustomFieldInputHelper customFieldInputHelper, FieldVisibilityManager fieldVisibilityManager) {
        this.fieldVisibilityManager = fieldVisibilityManager;
        this.jqlOperandResolver = jqlOperandResolver;
        this.customFieldInputHelper = notNull("customFieldInputHelper", customFieldInputHelper);
    }

    public CustomFieldSearcherClauseHandler getCustomFieldSearcherClauseHandler() {
        if (customFieldSearcherClauseHandler == null) {
            throw new IllegalStateException("Attempt to retrieve customFieldSearcherClauseHandler off uninitialised custom field searcher.");
        }
        return customFieldSearcherClauseHandler;
    }

    public SearcherInformation<CustomField> getSearchInformation() {
        if (searcherInformation == null) {
            throw new IllegalStateException("Attempt to retrieve SearcherInformation off uninitialised custom field searcher.");
        }
        return searcherInformation;
    }

    public SearchInputTransformer getSearchInputTransformer() {
        if (searchInputTransformer == null) {
            throw new IllegalStateException("Attempt to retrieve searchInputTransformer off uninitialised custom field searcher.");
        }
        return searchInputTransformer;
    }

    public SearchRenderer getSearchRenderer() {
        if (searchRenderer == null) {
            throw new IllegalStateException("Attempt to retrieve searchRenderer off uninitialised custom field searcher.");
        }
        return searchRenderer;
    }

    public LuceneFieldSorter getSorter(CustomField customField) {
        return new TextFieldSorter(customField.getId());
    }

    public String getSortField(CustomField customField) {
        return customField.getId();
    }

    public void init(CustomField field) {
        final ClauseNames names = field.getClauseNames();
        final FieldIndexer indexer = new ToDoFieldIndexer(fieldVisibilityManager, field);
        final IndexValueConverter indexValueConverter = new SimpleIndexValueConverter(false);
        JqlSelectOptionsUtil jqlSelectOptionsUtil = ComponentManager.getComponentInstanceOfType(JqlSelectOptionsUtil.class);
        QueryContextConverter queryContextConverter = new QueryContextConverter();

        searcherInformation = new CustomFieldSearcherInformation(
            field.getId(),
            field.getNameKey(),
            Collections.<FieldIndexer>singletonList(indexer),
            new AtomicReference<CustomField>(field));
        searchInputTransformer = new ToDoFieldSearchInputTransformer(
            field.getId(),
            names,
            field,
            jqlOperandResolver,
            jqlSelectOptionsUtil,
            queryContextConverter,
            customFieldInputHelper);
        searchRenderer = new ToDoFieldRenderer(
            names,
            getDescriptor(),
            field,
            new ToDoFieldValueProvider(),
            fieldVisibilityManager);
        final Set<Operator> supportedOperators = OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY;
        this.customFieldSearcherClauseHandler = new SimpleCustomFieldSearcherClauseHandler(
            new ExactTextCustomFieldValidator(),
            new ActualValueCustomFieldClauseQueryFactory(field.getId(), jqlOperandResolver, indexValueConverter, false),
            supportedOperators,
            JiraDataTypes.TEXT);
    }
}
