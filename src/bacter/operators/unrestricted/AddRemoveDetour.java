/*
 * Copyright (C) 2015 Tim Vaughan <tgvaughan@gmail.com>
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

package bacter.operators.unrestricted;

import bacter.Conversion;
import beast.core.Description;
import beast.evolution.tree.Node;
import beast.util.Randomizer;

import java.util.ArrayList;
import java.util.List;

@Description("This operator replaces a single conversion with a pair of" +
        " conversions, causing the lineage to temporarily visit another" +
        " clonal frame edge.")
public class AddRemoveDetour extends ConversionCreationOperator {

        @Override
        public double proposal() {
                if (Randomizer.nextBoolean())
                        return addDetour();
                else
                        return removeDetour();
        }

        /**
         * Detour addition variant of move.
         * @return log HGF
         */
        private double addDetour() {
                double logHGF = 0.0;

                // Select conversion at random
                Conversion conv = acg.getConversions().get(
                        Randomizer.nextInt(acg.getConvCount()));
                logHGF -= Math.log(1.0/acg.getConvCount());

                // Select detour times:
                double t1 = conv.getHeight1()
                        + Randomizer.nextDouble()*(conv.getHeight2()-conv.getHeight1());
                double t2 = conv.getHeight1()
                        + Randomizer.nextDouble()*(conv.getHeight2()-conv.getHeight1());

                double tLower, tUpper;
                if (t1 < t2) {
                        tLower = t1;
                        tUpper = t2;
                } else {
                        tLower = t2;
                        tUpper = t1;
                }

                logHGF -= Math.log(0.5) - 2.0*Math.log(conv.getHeight2()-conv.getHeight1());

                // Select non-root node below detour edge at random
                Node detour = acg.getNode(Randomizer.nextInt(acg.getNodeCount()-1));
                logHGF -= Math.log(1.0/(acg.getNodeCount()-1));

                // Abort if selected detour edge does not contain tLower and tUpper
                if (detour.getHeight() > tLower || detour.getParent().getHeight() < tUpper)
                        return Double.NEGATIVE_INFINITY;

                // Abort if conv is already attached to selected detour edge:
                if (detour == conv.getNode1() || detour == conv.getNode2())
                        return Double.NEGATIVE_INFINITY;

                Conversion convA = new Conversion();
                convA.setNode1(conv.getNode1());
                convA.setHeight1(conv.getHeight1());
                convA.setNode2(detour);
                convA.setHeight2(tLower);
                convA.setStartSite(conv.getStartSite());
                convA.setEndSite(conv.getEndSite());

                Conversion convB = new Conversion();
                convB.setNode1(detour);
                convB.setHeight1(tUpper);
                convB.setNode2(conv.getNode2());
                convB.setHeight2(conv.getHeight1());
                logHGF -= drawAffectedRegion(convB);

                acg.deleteConversion(conv);
                acg.addConversion(convA);
                acg.addConversion(convB);

                // Count number of node1s and node2s attached to detour edge
                int node1Count = 0;
                int node2Count = 0;
                for (Conversion thisConv : acg.getConversions()) {
                        if (thisConv.getNode1() == thisConv.getNode2())
                                continue;

                        if (thisConv.getNode1() == detour)
                                node1Count += 1;

                        if (thisConv.getNode2() == detour)
                                node2Count += 1;
                }

                // Incorporate probability of reverse move:
                logHGF += Math.log(1.0/((acg.getNodeCount()-1)*node1Count*node2Count));

                return logHGF;
        }

        /**
         * Detour deletion variant of move.
         * @return log HGF
         */
        private double removeDetour() {
                double logHGF = 0.0;

                // Choose non-root detour edge at random
                Node detour = acg.getNode(Randomizer.nextInt(acg.getNodeCount()-1));
                logHGF -= Math.log(1.0/(acg.getNodeCount()-1));

                List<Conversion> convApotentials = new ArrayList<>();
                List<Conversion> convBpotentials = new ArrayList<>();

                for (Conversion conv : acg.getConversions()) {
                        if (conv.getNode1() == conv.getNode2())
                                continue;

                        if (conv.getNode2() == detour)
                                convApotentials.add(conv);

                        if (conv.getNode1() == detour)
                                convBpotentials.add(conv);
                }

                Conversion convA = convApotentials.get(Randomizer.nextInt(convApotentials.size()));
                Conversion convB = convBpotentials.get(Randomizer.nextInt(convApotentials.size()));

                if (convA.getHeight2()>convB.getHeight1())
                        return Double.NEGATIVE_INFINITY;

                logHGF -= Math.log(1.0 / (convApotentials.size() * convBpotentials.size()));

                double tLowerBound = convA.getHeight1();
                double tUpperBound = convB.getHeight2();

                Conversion conv = new Conversion();
                conv.setNode1(convA.getNode1());
                conv.setHeight1(convA.getHeight1());
                conv.setNode2(convB.getNode2());
                conv.setHeight2(convB.getHeight2());
                conv.setStartSite(convA.getStartSite());
                conv.setEndSite(convA.getEndSite());

                acg.deleteConversion(convA);
                acg.deleteConversion(convB);
                acg.addConversion(conv);

                logHGF += Math.log(1.0/acg.getConvCount())
                        + Math.log(0.5) - 2.0*Math.log(tUpperBound-tLowerBound)
                        + getAffectedRegionProb(convB);

                return logHGF;
        }
}
