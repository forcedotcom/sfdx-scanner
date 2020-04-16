import { Container as IOC} from "inversify";
import "reflect-metadata";

export const Services = {
	RuleEngine: Symbol("RuleEngine")
};

import {PmdEngine} from './lib/pmd/PmdEngine';
import {RuleEngine} from './lib/services/RuleEngine';

export const Container = new IOC();
Container.bind<RuleEngine>(Services.RuleEngine).to(PmdEngine);
