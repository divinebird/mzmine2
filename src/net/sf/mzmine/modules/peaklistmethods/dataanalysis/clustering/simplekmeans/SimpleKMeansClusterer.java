/*
 * Copyright 2006-2012 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.simplekmeans;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringAlgorithm;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringResult;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.em.EMClustererParameters;
import net.sf.mzmine.parameters.ParameterSet;
import weka.clusterers.SimpleKMeans;
import weka.core.Instance;
import weka.core.Instances;

public class SimpleKMeansClusterer implements ClusteringAlgorithm {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final String MODULE_NAME = "Simple KMeans";

    @Override
    public String getName() {
	return MODULE_NAME;
    }

    @Override
    public ClusteringResult performClustering(Instances dataset,
	    ParameterSet parameters) {

	List<Integer> clusters = new ArrayList<Integer>();
	String[] options = new String[2];
	SimpleKMeans clusterer = new SimpleKMeans();

	int numberOfGroups = parameters.getParameter(
		SimpleKMeansClustererParameters.numberOfGroups).getValue();
	options[0] = "-N";
	options[1] = String.valueOf(numberOfGroups);

	try {
	    clusterer.setOptions(options);
	    clusterer.buildClusterer(dataset);
	    Enumeration e = dataset.enumerateInstances();
	    while (e.hasMoreElements()) {
		clusters.add(clusterer.clusterInstance((Instance) e
			.nextElement()));
	    }
	    ClusteringResult result = new ClusteringResult(clusters, null,
		    clusterer.numberOfClusters(), parameters.getParameter(
			    EMClustererParameters.visualization).getValue());
	    return result;

	} catch (Exception ex) {
	    logger.log(Level.SEVERE, null, ex);
	    return null;
	}
    }

    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {
	return SimpleKMeansClustererParameters.class;
    }
}
