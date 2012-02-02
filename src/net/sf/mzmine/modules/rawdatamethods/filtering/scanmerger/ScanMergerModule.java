/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.scanmerger;

import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Scan merger module.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ScanMergerModule implements MZmineProcessingModule {

    // Module name.
    private static final String MODULE_NAME = "Scan merger";
    private static final String MODULE_DESCRIPTION = "Merges merges multiple raw data files";

    @Nonnull
    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {

        return ScanMergerParameters.class;
    }

    @Nonnull
    @Override
    public String getName() {

        return MODULE_NAME;
    }

    @Nonnull
    @Override
    public String getDescription() {

        return MODULE_DESCRIPTION;
    }

    @Nonnull
    @Override
    public MZmineModuleCategory getModuleCategory() {

        return MZmineModuleCategory.RAWDATAFILTERING;
    }

    @Nonnull
    @Override
    public ExitCode runModule(@Nonnull final ParameterSet parameters, @Nonnull final Collection<Task> tasks) {

        tasks.add(new ScanMergerTask(parameters));
        return ExitCode.OK;
    }
}
