// The bypass evaluator, rather than this trigger, checks whether a method is active.
class PlaywrightGroovyEventResolver {
    def String run(service, registeredService, authentication, httpRequest, logger, ... args) {
        return "mfa-esupotp"
    }
}
