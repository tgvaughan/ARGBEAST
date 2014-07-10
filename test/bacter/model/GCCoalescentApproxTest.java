/*
 * Copyright (C) 2013 Tim Vaughan <tgvaughan@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package bacter.model;

import bacter.model.GCCoalescentApprox;
import bacter.Recombination;
import bacter.RecombinationGraph;
import bacter.TestBase;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.JukesCantor;
import beast.evolution.tree.Node;
import beast.evolution.tree.coalescent.ConstantPopulation;
import beast.evolution.tree.coalescent.TreeIntervals;
import beast.util.ClusterTree;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the evaluation of the ARG probability density under the approximate
 * coalescent with gene conversion model used by GCCCoalescentApprox.
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class GCCoalescentApproxTest extends TestBase {
    
    public GCCoalescentApproxTest() {
    }
    
    /**
     * Calculate absolute difference between two real numbers relative to
     * the mean of the two numbers.
     * 
     * @param a
     * @param b
     * @return relative difference
     */
    public double relativeDiff(double a, double b) {
        return 2.0*Math.abs((a-b)/(a+b));
    }
    
    @Test
    public void test() throws Exception {
        
        Alignment alignment = getAlignment();
        
        // RecombinationGraph
        RecombinationGraph arg = new RecombinationGraph();
        ClusterTree tree = new ClusterTree();
        tree.initByName(
                "clusterType", "upgma",
                "taxa", alignment);
        
        arg.assignFrom(tree);
        arg.initByName("alignment", alignment);
        
        // Site model:
        JukesCantor jc = new JukesCantor();
        jc.initByName();
        SiteModel siteModel = new SiteModel();
        siteModel.initByName(
                "substModel", jc);
        
        // Population size model:
        ConstantPopulation popFunction = new ConstantPopulation();
        popFunction.initByName("popSize", new RealParameter("15.0"));
        
        // Coalescent
        GCCoalescentApprox coalescent = new GCCoalescentApprox();
        coalescent.initByName(
                "arg", arg,
                "populationModel", popFunction,
                "rho", new RealParameter("1"),
                "delta", new RealParameter("10"));
        
        // Test converted region probability when no recombinations exist
        double logP = coalescent.calculateConvertedRegionMapLogP();
        double logPtrue = -0.6574230251814153;
        assertTrue(relativeDiff(logP, logPtrue)<1e-15);
        
        // Coalescent probability when no recombinations exist
        logP = coalescent.calculateClonalFrameLogP();
        logPtrue = -30.102417640665447;
        assertTrue(relativeDiff(logP, logPtrue)<1e-15);
        
        //Add a single recombination event
        Node node1 = arg.getExternalNodes().get(0);
        Node node2 = node1.getParent();
        double height1 = 0.5*(node1.getHeight() + node1.getParent().getHeight());
        double height2 = 0.5*(node2.getHeight() + node2.getParent().getHeight());
        int startLocus = 100;
        int endLocus = 200;
        Recombination newRecomb = new Recombination(node1, height1, node2, height2,
                startLocus, endLocus);
        arg.addRecombination(newRecomb);

        // Test converted region probability when one recombination exists
        logP = coalescent.calculateConvertedRegionMapLogP();
        logPtrue = -20.652012260825202;
        assertTrue(relativeDiff(logP, logPtrue)<1e-15);
        
        // Test coalescent probability when one recombination exists
        logP = coalescent.calculateRecombinantLogP(newRecomb);
        logPtrue = -3.004053776295126;
        assertTrue(relativeDiff(logP, logPtrue)<1e-15);
    }
}