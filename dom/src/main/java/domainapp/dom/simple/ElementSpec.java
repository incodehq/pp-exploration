/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package domainapp.dom.simple;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.VersionStrategy;

import com.google.common.collect.Lists;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Auditing;
import org.apache.isis.applib.annotation.Collection;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.ParameterLayout;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.Publishing;
import org.apache.isis.applib.services.i18n.TranslatableString;
import org.apache.isis.applib.services.message.MessageService;
import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.applib.services.title.TitleService;
import org.apache.isis.applib.util.ObjectContracts;

import lombok.Getter;
import lombok.Setter;

@javax.jdo.annotations.PersistenceCapable(
        identityType=IdentityType.DATASTORE,
        schema = "simple"
)
@javax.jdo.annotations.DatastoreIdentity(
        strategy=javax.jdo.annotations.IdGeneratorStrategy.IDENTITY,
         column="id")
@javax.jdo.annotations.Version(
        strategy= VersionStrategy.DATE_TIME,
        column="version")
@javax.jdo.annotations.Queries({
//        @javax.jdo.annotations.Query(
//                name = "findBySegmentAndPosition", language = "JDOQL",
//                value = "SELECT "
//                        + "FROM domainapp.dom.simple.ElementSpec "
//                        + "WHERE segment == :segment && position == :position")
})
@javax.jdo.annotations.Unique(name="ElementSpec_segment_position_UNQ", members = {"segment", "position"})
@DomainObject(
        publishing = Publishing.ENABLED,
        auditing = Auditing.ENABLED
)
public class ElementSpec implements Comparable<ElementSpec> {

    //region > title
    public TranslatableString title() {
        return TranslatableString.tr("{segment} [{position}]", "segment", getSegment().getSegmentId(), "position", getPosition());
    }
    //endregion

    //region > constructor
    public ElementSpec(final Segment segment, final int position) {
        setSegment(segment);
        setPosition(position);
    }
    //endregion

    @Column(allowsNull = "false")
    @Property()
    @Getter @Setter
    private Segment segment;

    @Column(allowsNull = "false")
    @Property()
    @Getter @Setter
    private int position;

    @Column(allowsNull = "true")
    @Property()
    @PropertyLayout(multiLine = 10)
    @Getter @Setter
    private String specialNotes;

    @Join
    @Element(dependent = "false")
    @Collection()
    @Getter @Setter
    private SortedSet<ProductionStepSpec> steps = new TreeSet<ProductionStepSpec>();

    @Action
    public ConcreteElement commission(
            @ParameterLayout(named = "Production Id")
            final String productionId) {
        return concreteElementRepository.createFor(this, productionId);
    }

    //region > associateBasicSteps

    @Action
    @ActionLayout(named = "Basic", describedAs = "Associate basic steps to this element")
    @MemberOrder(name = "steps", sequence = "1")
    public ElementSpec associateBasicSteps() {
        final List<ProductionStepSpec> steps = productionStepSpecRepository.findByType(ProductionStepType.BASIC);
        getSteps().addAll(steps);
        return this;
    }

    public boolean hideAssociateBasicSteps() {
        return !getSteps().isEmpty();
    }

    //endregion

    //region > addStep
    @Action
    @ActionLayout(named = "Add")
    @MemberOrder(name = "steps", sequence = "2")
    public ElementSpec addStep(final ProductionStepSpec step) {
        getSteps().add(step);
        return this;
    }

    public List<ProductionStepSpec> choices0AddStep() {
        final List<ProductionStepSpec> steps = Lists.newArrayList(productionStepSpecRepository.listAll());
        steps.removeAll(getSteps());
        return steps;
    }

    //endregion

    //region > removeStep

    @Action
    @ActionLayout(named = "Remove")
    @MemberOrder(name = "steps", sequence = "3")
    public ElementSpec removeStep(final ProductionStepSpec step) {
        getSteps().add(step);
        return this;
    }

    public List<ProductionStepSpec> choices0RemoveStep() {
        return Lists.newArrayList(getSteps());
    }

    //endregion

    //region > next, previous

    @Action()
    public ElementSpec next() {
        return getSegment().elementAfter(this);
    }

    @Action()
    public ElementSpec previous() {
        return getSegment().elementBefore(this);
    }

    //endregion

    //region > toString, compareTo
    @Override
    public String toString() {
        return ObjectContracts.toString(this, "segment", "position");
    }
    @Override
    public int compareTo(final ElementSpec other) {
        return ObjectContracts.compare(this, other, "segment", "position");
    }

    //endregion

    //region > injected dependencies

    @javax.inject.Inject
    RepositoryService repositoryService;

    @javax.inject.Inject
    TitleService titleService;

    @javax.inject.Inject
    MessageService messageService;

    //endregion

    @Inject
    ProductionStepSpecRepository productionStepSpecRepository;
    @Inject
    ConcreteElementRepository concreteElementRepository;


}
